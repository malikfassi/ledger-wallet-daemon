package co.ledger.wallet.daemon.models

import co.ledger.core
import co.ledger.core.implicits
import co.ledger.core.implicits._
import co.ledger.wallet.daemon.exceptions.InvalidArgumentException
import co.ledger.wallet.daemon.models.Account.{Account, Derivation}
import co.ledger.wallet.daemon.schedulers.observers.SynchronizationResult
import co.ledger.wallet.daemon.services.LogMsgMaker
import co.ledger.wallet.daemon.{DaemonConfiguration, utils}
import co.ledger.wallet.daemon.utils.{AsArrayList, HexUtils}
import com.fasterxml.jackson.annotation.JsonProperty
import com.twitter.inject.Logging

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

class Wallet(private val coreW: core.Wallet)(implicit ec: ExecutionContext) extends Logging {
  implicit def asArrayList[T](input: Seq[T]): AsArrayList[T] = new AsArrayList[T](input)

  private val configuration: Map[String, Any] = Map[String, Any]()

  val walletName: String = coreW.getName

  lazy val walletView: Future[WalletView] = {
    coreW.getAccountCount().flatMap { count =>
      getBalance(count).map { balance =>
        WalletView(coreW.getName, count, balance, currency.currencyView, configuration)
      }
    }
  }

  lazy val currency: Currency = Currency.newInstance(coreW.getCurrency)

  // library call expensive (db calls) TODO
  protected def lastBlockHeight: Future[Long] = {
    coreW.getLastBlock().map { block => block.getHeight}
  }

  def account(index: Int): Future[Option[Account]] = {
    coreW.getAccount(index).map { coreA => Option(Account.newInstance(coreA, coreW)) }.recover {
      case e: implicits.AccountNotFoundException => None
    }
  }

  def accountCreationInfo(index: Option[Int]): Future[Derivation] = {
    (index match {
      case Some(i) => coreW.getAccountCreationInfo(i)
      case None => coreW.getNextAccountCreationInfo()
    }).map { info => Account.newDerivation(info) }
  }


  def accounts(): Future[Seq[Account]] = {
    coreW.getAccountCount().flatMap { count =>
      coreW.getAccounts(0, count).map { coreAs =>
        coreAs.asScala.toSeq.map(Account.newInstance(_, coreW))
      }
    }
  }

  def upsertAccount(accountDerivations: AccountDerivationView): Future[Account] = {
    val accountCreationInfo = new core.AccountCreationInfo(
      accountDerivations.accountIndex,
      (for (derivationResult <- accountDerivations.derivations) yield derivationResult.owner).asArrayList,
      (for (derivationResult <- accountDerivations.derivations) yield derivationResult.path).asArrayList,
      (for (derivationResult <- accountDerivations.derivations) yield HexUtils.valueOf(derivationResult.pubKey.get)).asArrayList,
      (for (derivationResult <- accountDerivations.derivations) yield HexUtils.valueOf(derivationResult.chainCode.get)).asArrayList
    )
    coreW.newAccountWithInfo(accountCreationInfo).map { coreA =>
      Future.successful(Account.newInstance(coreA, coreW))
    }.recover {
      case e: implicits.InvalidArgumentException => Future.failed(InvalidArgumentException(e.getMessage, e))
      case alreadyExist: implicits.AccountAlreadyExistsException =>
        warn(LogMsgMaker.newInstance("Account already exist").append("index", accountDerivations.accountIndex).toString())
        coreW.getAccount(accountDerivations.accountIndex).map(Account.newInstance(_, coreW))
    }.flatten.map { a =>
      if(DaemonConfiguration.realtimeObserverOn) a.startRealTimeObserver()
      a
    }
  }

  def syncWallet(poolName: String)(implicit coreEC: core.ExecutionContext): Future[Seq[SynchronizationResult]] = {
    accounts().flatMap { accounts =>
      Future.sequence(accounts.map { account => account.syncAccount(poolName)})
    }
  }

  def startRealTimeObserver(): Unit = {
    accounts().map { as => as.foreach(_.startRealTimeObserver()) }
  }

  def stopRealTimeObserver(): Unit = {
    accounts().map { as => as.foreach(_.stopRealTimeObserver()) }
  }

  private def getBalance(count: Int): Future[Long] = {
    coreW.getAccounts(0, count) flatMap { (accounts) =>
      val accs = accounts.asScala.toList
      val balances = Future.sequence(for (acc <- accs) yield acc.getBalance())
      balances.map { bs =>
        val bls = for (balance <- bs) yield balance.toLong
        utils.sum(bls)
      }
    }
  }
}

object Wallet {
  def newInstance(coreW: core.Wallet)(implicit ec: ExecutionContext): Wallet = {
    new Wallet(coreW)
  }
}

case class WalletView(
                       @JsonProperty("name") name: String,
                       @JsonProperty("account_count") accountCount: Int,
                       @JsonProperty("balance") balance: Long,
                       @JsonProperty("currency") currency: CurrencyView,
                       @JsonProperty("configuration") configuration: Map[String, Any]
                     )

case class WalletsViewWithCount(count: Int, wallets: Seq[WalletView])