package com.sparkfits

import org.scalatest.{BeforeAndAfterAll, FunSuite, FlatSpec, Matchers}
import org.scalatest.Matchers._

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame

import org.apache.log4j.Level
import org.apache.log4j.Logger

import nom.tam.fits.Fits

import com.sparkfits.fits._

/**
  * Test class for the package object.
  */
class packageTest extends FunSuite with BeforeAndAfterAll {

  // Set to Level.WARN is you want verbosity
  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("akka").setLevel(Level.OFF)

  private val master = "local[2]"
  private val appName = "sparkfitsTest"

  private var spark : SparkSession = _

  override protected def beforeAll() : Unit = {
    super.beforeAll()
    spark = SparkSession
      .builder()
      .master(master)
      .appName(appName)
      .getOrCreate()
  }

  override protected def afterAll(): Unit = {
    try {
      spark.sparkContext.stop()
    } finally {
      super.afterAll()
    }
  }
  // END TODO

  // Add more and put a loop for several tests!
  val fn = "src/test/resources/test.fits"

  // Test if readfits does nothing :D
  test("Readfits test: Do you send back a FitsContext?") {
    val results = spark.readfits
    assert(results.isInstanceOf[FitsContext])
  }

  // Test if options grab what we give to it
  test("Option test: can you record a new argument?") {
    val results = spark.readfits.option("toto", "tutu")
    assert(results.extraOptions.contains("toto"))
  }

  // Test if options grab a String
  test("Option test: can you record a String value?") {
    val results = spark.readfits.option("toto", "tutu")
    assert(results.extraOptions("toto").contains("tutu"))
  }

  // Test if options grab a Double
  test("Option test: can you record a Double value?") {
    val results = spark.readfits.option("toto", 3.0)
    assert(results.extraOptions("toto").contains("3.0"))
  }

  // Test if options grab a Long
  test("Option test: can you record a Long value?") {
    val results = spark.readfits.option("toto", 3)
    assert(results.extraOptions("toto").contains("3"))
  }

  // Test if options grab a Boolean
  test("Option test: can you record a Boolean value?") {
    val results = spark.readfits.option("toto", true)
    assert(results.extraOptions("toto").contains("true"))
  }

  // Test yieldRows
  test("yieldRows test: can you spit a bunch of rows?") {
    val f = new Fits(fn)
    val results = spark.yieldRows(f, 1, 0, 10, 100)
    assert(results.size == 10)
  }

  // Test yieldRows
  test("yieldRows test: are you aware of the end of the table data?") {
    val f = new Fits(fn)
    val results = spark.yieldRows(f, 1, 8, 12, 100)
    assert(results.size == 4)
  }

  // Test DataFrame
  test("DataFrame test: can you really make a DF from the hdu?") {
    val results = spark.readfits
      .option("datatype", "table")
      .option("HDU", 1)
      .load(fn)
    assert(results.isInstanceOf[DataFrame])
  }
}
