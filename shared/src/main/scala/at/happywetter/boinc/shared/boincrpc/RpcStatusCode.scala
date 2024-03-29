package at.happywetter.boinc.shared.boincrpc

object RpcStatusCode extends Enumeration {
  type RpcStatusCode = Value
  val BOINC_SUCCESS = Value(0)
  val ERR_SELECT = Value(-100)
  val ERR_READ = Value(-101)
  val ERR_WRITE = Value(-103)
  val ERR_FREAD = Value(-104)
  val ERR_FWRITE = Value(-105)
  val ERR_IO = Value(-106)
  val ERR_CONNECT = Value(-107)
  val ERR_FOPEN = Value(-108)
  val ERR_RENAME = Value(-109)
  val ERR_UNLINK = Value(-110)
  val ERR_OPENDIR = Value(-110)
  val ERR_XML_PARSE = Value(-112)
  val ERR_GETHOSTBYNAME = Value(-113)
  val ERR_GIVEUP_DOWNLOAD = Value(-114)
  val ERR_GIVEUP_UPLOAD = Value(-115)
  val ERR_NULL = Value(-116)
  val ERR_NEG = Value(-117)
  val ERR_BUFFER_OVERFLOW = Value(-118)
  val ERR_MD5_FAILED = Value(-119)
  val ERR_RSA_FAILED = Value(-120)
  val ERR_NO_SIGNATURE = Value(-123)
  val ERR_THREAD = Value(-124)
  val ERR_SIGNAL_CATCH = Value(-125)
  val ERR_UPLOAD_TRANSIENT = Value(-127)
  val ERR_UPLOAD_PERMANENT = Value(-128)
  val ERR_IDLE_PERIOD = Value(-129)
  val ERR_ALREADY_ATTACHED = Value(-130)


}
