package com.evidentid.database

import akka.http.scaladsl.model.DateTime
import com.github.tminglei.slickpg._
import slick.basic.Capability
import slick.jdbc._

import java.sql.{Date, Time, Timestamp}
import java.time.Duration
import java.util.Calendar
import scala.concurrent.duration.{DAYS, FiniteDuration, HOURS, MINUTES, SECONDS}

trait DatabaseProfile
    extends ExPostgresProfile
    with PgJsonSupport
    with PgCirceJsonSupport
    with PgDateSupport
    with PgDate2Support
    with PgArraySupport {

  def pgjson = "jsonb"

  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  override val api = Api

  object Api extends API with JsonImplicits with CirceImplicits with DateTimeImplicits with SimpleDateTimeImplicits with ArrayImplicits {

    import slick.ast.Library._
    import slick.ast._
    import slick.lifted.FunctionSymbolExtensionMethods._

    import scala.language.implicitConversions

    implicit def simpleTimestampColumnExtensionMethodsDateTime(
      c: Rep[DateTime]
    ): TimestampColumnExtensionMethods[Date, Time, DateTime, Calendar, Duration, DateTime] =
      new TimestampColumnExtensionMethods[Date, Time, DateTime, Calendar, Duration, DateTime](c)

    implicit def simpleTimestampOptColumnExtensionMethodsDateTime(
      c: Rep[Option[DateTime]]
    ): TimestampColumnExtensionMethods[Date, Time, DateTime, Calendar, Duration, Option[DateTime]] =
      new TimestampColumnExtensionMethods[Date, Time, DateTime, Calendar, Duration, Option[DateTime]](c)

    implicit object SetFiniteDuration extends SetParameter[FiniteDuration] {

      def apply(v: FiniteDuration, pp: PositionedParameters): Unit = {
        val interval = v.unit match {
          case DAYS    => Interval(0, 0, v.length.toInt, 0, 0, 0)
          case HOURS   => Interval(0, 0, 0, v.length.toInt, 0, 0)
          case MINUTES => Interval(0, 0, 0, 0, v.length.toInt, 0)
          case SECONDS => Interval(0, 0, 0, 0, 0, v.length.toInt)
          case _       => Interval(0, 0, 0, 0, 0, v.toSeconds.toDouble)
        }
        pp.setString(interval.toString())
      }

    }

    // Declare the name of an aggregate function:
    val ArrayAgg = new SqlAggregateFunction("array_agg")

    // Implement the aggregate function as an extension method:
    implicit class ArrayAggColumnQueryExtensionMethods[P, C[_]](val q: Query[Rep[P], _, C]) {

      def arrayAgg[B](tm: TypedType[List[B]]): Rep[List[B]] =
        ArrayAgg.column[List[B]](q.toNode)(tm)

    }

    val JsonbBuildObject = new SqlFunction("jsonb_build_object")

    def jsonbBuildObject[T: TypedType](fields: (String, Rep[_])*): Rep[T] =
      JsonbBuildObject.column[T](fields.flatMap { case key -> value => Seq(key.toNode, value.toNode) }: _*)

    implicit val timestampMapper: JdbcType[DateTime] with BaseTypedType[DateTime] =
      MappedColumnType.base[DateTime, Timestamp](dt => new Timestamp(dt.clicks), ts => DateTime(ts.getTime))

    final val now: Rep[DateTime] = SimpleFunction.nullary[DateTime]("NOW")

    implicit class StreamingOps[R, T](action: StreamingProfileAction[R, T, Effect.Read]) {

      // According to Slick documentation, some database systems (for example Postgres) require additional streaming
      // parameters in order to work properly without caching all data on client side: https://scala-slick.org/doc/3.2.0/dbio.html#streaming
      def withoutCaching: DBIOAction[R, DatabaseProfile.Api.Streaming[T], Effect.Read with Effect.Transactional] =
        action
          .withStatementParameters(rsType = ResultSetType.ForwardOnly, rsConcurrency = ResultSetConcurrency.ReadOnly, fetchSize = 1000)
          .transactionally

    }

    object JsonTypes {

      final val string = "string"
      final val array = "array"
      final val `object` = "object"
      final val number = "number"
      final val boolean = "boolean"
      final val `null` = "null"
    }

  }

}

object DatabaseProfile extends DatabaseProfile
