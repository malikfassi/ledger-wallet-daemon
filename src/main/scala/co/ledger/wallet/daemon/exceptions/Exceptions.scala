package co.ledger.wallet.daemon.exceptions

import java.util.UUID

case class ERC20NotFoundException(contract: String) extends {
  val msg = s"No ERC20 token $contract in your account"
  val code = ErrorCodes.ERC20_NOT_FOUND
} with Exception(msg) with DaemonException

case class ERC20BalanceNotEnough(tokenAddress: String, balance: BigInt, need: BigInt)
  extends {
    val msg = s"Not enough funds on ERC20 ($tokenAddress) account: having $balance, need $need"
    val code = ErrorCodes.ERC20_BALANCE_NOT_ENOUGH
  } with Exception(msg) with DaemonException

case class AccountNotFoundException(accountIndex: Int) extends {
  val msg = s"Account with index $accountIndex doesn't exist"
  val code = ErrorCodes.ACCOUNT_NOT_FOUND
} with Exception(msg) with DaemonException

case class OperationNotFoundException(cursor: UUID) extends {
  val msg = s"Operation with previous or next cursor $cursor doesn't exist"
  val code = ErrorCodes.OPERATION_NOT_FOUND
} with Exception(msg) with DaemonException

case class WalletNotFoundException(walletName: String) extends {
  val msg = s"Wallet $walletName doesn't exist"
  val code = ErrorCodes.WALLET_NOT_FOUND
} with Exception(msg) with DaemonException

case class WalletPoolNotFoundException(poolName: String) extends {
  val msg = s"Wallet pool $poolName doesn't exist"
  val code = ErrorCodes.WALLET_POOL_NOT_FOUND
} with Exception(msg) with DaemonException

case class WalletPoolAlreadyExistException(poolName: String) extends {
  val msg = s"Wallet pool $poolName already exists"
  val code = ErrorCodes.WALLET_POOL_ALREADY_EXIST
} with Exception(msg) with DaemonException

case class CurrencyNotFoundException(currencyName: String) extends {
  val msg = s"Currency $currencyName is not found"
  val code = ErrorCodes.CURRENCY_NOT_FOUND
} with Exception(msg) with DaemonException

case class CurrencyNotSupportedException(currencyName: String) extends {
  val msg = s"Currency $currencyName is not supported"
  val code = ErrorCodes.CURRENCY_NOT_SUPPORTED
} with Exception(msg) with DaemonException

case class UserNotFoundException(pubKey: String) extends {
  val msg = s"User $pubKey doesn't exist"
  val code = ErrorCodes.USER_NOT_FOUND
} with Exception(msg) with DaemonException

case class UserAlreadyExistException(pubKey: String) extends {
  val msg = s"User $pubKey already exists"
  val code = ErrorCodes.USER_ALREADY_EXIST
} with Exception(msg) with DaemonException

case class InvalidCurrencyForErc20Operation() extends {
  val msg = "ERC20 Operation with non ETH account"
  val code = ErrorCodes.INVALID_CURRENCY_FOR_ERC20
} with Exception(msg) with DaemonException

case class CoreBadRequestException(msg: String, t: Throwable) extends Exception(msg, t) with DaemonException {
  def code: Int = ErrorCodes.CORE_BAD_REQUEST
}

case class DaemonDatabaseException(msg: String, t: Throwable) extends Exception(msg, t) with DaemonException {
  def code: Int = ErrorCodes.DAEMON_DATABASE_EXCEPTION
}

case class SignatureSizeUnmatchException(txSize: Int, signatureSize: Int) extends {
  val code = ErrorCodes.SIGNATURE_SIZE_UNMATCH
  val msg = "Signatures and transaction inputs size not matching"
} with Exception(msg) with DaemonException

trait DaemonException {
  def code: Int

  def msg: String
}

object ErrorCodes {
  val SIGNATURE_SIZE_UNMATCH = 101
  val ERC20_BALANCE_NOT_ENOUGH = 102
  val ERC20_NOT_FOUND = 200
  val ACCOUNT_NOT_FOUND = 201
  val OPERATION_NOT_FOUND = 202
  val WALLET_NOT_FOUND = 203
  val WALLET_POOL_NOT_FOUND = 204
  val WALLET_POOL_ALREADY_EXIST = 205
  val CURRENCY_NOT_FOUND = 206
  val USER_NOT_FOUND = 207
  val USER_ALREADY_EXIST = 208
  val CURRENCY_NOT_SUPPORTED = 209
  val INVALID_CURRENCY_FOR_ERC20 = 210
  val CORE_BAD_REQUEST = 301
  val DAEMON_DATABASE_EXCEPTION = 302
}
