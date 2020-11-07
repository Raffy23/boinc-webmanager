package at.happywetter.boinc.util.quill

import io.getquill.{H2MonixJdbcContext, SnakeCase}


/**
 * Created by: 
 *
 * @author Raphael
 * @version 07.11.2020
 */
object ArrayCodec {

  def stringArrayEncoder(implicit ctx: H2MonixJdbcContext[SnakeCase]): ctx.Encoder[Array[String]] = {
    import ctx._
    encoder(java.sql.Types.ARRAY, (index, value, row) =>
      row.setObject(index, value, java.sql.Types.ARRAY)
    )
  }

  def stringArrayDecoder(implicit ctx: H2MonixJdbcContext[SnakeCase]): ctx.Decoder[Array[String]] = {
    import ctx._
    decoder((index, row) => row.getObject(index).asInstanceOf[Array[String]])
  }

}
