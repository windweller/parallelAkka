package SECProject.Database

import com.typesafe.config.ConfigFactory
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.meta.MTable

object Companies {

  case class Company (id: Option[Int], companyName: Option[String], companyAbbr: Option[String], quandlTag: Option[String],
                      companyType: Option[String], CIK: Option[String], formType: Option[String],
                      quarter: Option[String], year: Option[Short], date: Option[String],
                      url: Option[String], fileName: Option[String], docDiskLoc: Option[String],
                      BOWVector: Option[String],ModelVector: Option[String], stockTrend: Option[String])

  class CompanyTable(tag: Tag) extends Table[Company](tag, "Companies") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def companyName = column[Option[String]]("COMPANY_NAME")
    def companyAbbr = column[Option[String]]("COMPANY_ABBR")
    def quandlTag = column[Option[String]]("QUANDL_TAG")
    def companyType = column[Option[String]]("COMPANY_TYPE") //the industry this company is in, come from Yahoo
    def CIK = column[Option[String]]("CIK")
    def formType = column[Option[String]]("FORM_TYPE")
    def quarter = column[Option[String]]("QUARTER")
    def year = column[Option[Short]]("YEAR")
    def date = column[Option[String]]("DATE") //formatted as "2012-02-02"
    def url = column[Option[String]]("URL")
    def fileName = column[Option[String]]("FILE_NAME")
    def docDiskLoc = column[Option[String]]("DOC_DISK_LOC")
    def BOWVector = column[Option[String]]("BOW_VECTOR")
    def ModelVector = column[Option[String]]("MODEL_VECTOR")
    def stockTrend = column[Option[String]]("STOCK_TREND")

    def * = (id.?, companyName, companyAbbr, quandlTag, companyType, CIK, formType,
      quarter, year, date, url, fileName, docDiskLoc, BOWVector, ModelVector, stockTrend) <> (Company.tupled, Company.unapply)
  }

  val companies = TableQuery[CompanyTable]

}

object DAL {
  val config = ConfigFactory.load()
  val db = Database.forURL(url = config.getString("db.url"),
    user = config.getString("db.user"), password = config.getString("db.password"), driver = config.getString("db.driver"))

  def databaseInit() {
    db.withSession { implicit session =>
      if (MTable.getTables("Companies").list().isEmpty) {
        Companies.companies.ddl.create
      }
    }
  }
}