/*
 * Copyright 2018 Julien Peloton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sparkfits

// Low level import
import scala.util.{Try, Success, Failure}

// Hadoop import
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.LongWritable

// Spark import
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Row
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._

// Internal import
import com.sparkfits.FitsSchema._
import com.sparkfits.FitsLib.FitsBlock
import com.sparkfits.FitsFileInputFormat._

package object fits {

  /**
   * Adds a method, `fitsFile`, to SparkSession that allows reading FITS data.
   * Note that for the moment, we provide support only for FITS table.
   * We will add FITS image later on.
   *
   * The interpreter session below shows how to use basic functionalities:
   *
   * {{{
   * scala> val fn = "src/test/resources/test_file.fits"
   * scala> val df = spark.readfits
   *  .option("datatype", "table")
   *  .option("HDU", 1)
   *  .option("printHDUHeader", true)
   *  .load(fn)
   * +------ HEADER (HDU=1) ------+
   * XTENSION= BINTABLE           / binary table extension
   * BITPIX  =                    8 / array data type
   * NAXIS   =                    2 / number of array dimensions
   * NAXIS1  =                   34 / length of dimension 1
   * NAXIS2  =                20000 / length of dimension 2
   * PCOUNT  =                    0 / number of group parameters
   * GCOUNT  =                    1 / number of groups
   * TFIELDS =                    5 / number of table fields
   * TTYPE1  = target
   * TFORM1  = 10A
   * TTYPE2  = RA
   * TFORM2  = E
   * TTYPE3  = Dec
   * TFORM3  = D
   * TTYPE4  = Index
   * TFORM4  = K
   * TTYPE5  = RunId
   * TFORM5  = J
   * END
   * +----------------------------+
   * df: org.apache.spark.sql.DataFrame = [target: string, RA: float ... 3 more fields]
   *
   * scala> df.printSchema
   * root
   *  |-- target: string (nullable = true)
   *  |-- RA: float (nullable = true)
   *  |-- Dec: double (nullable = true)
   *  |-- Index: long (nullable = true)
   *  |-- RunId: integer (nullable = true)
   *
   * scala> df.show(5)
   * +----------+---------+--------------------+-----+-----+
   * |    target|       RA|                 Dec|Index|RunId|
   * +----------+---------+--------------------+-----+-----+
   * |NGC0000000| 3.448297| -0.3387486324784641|    0|    1|
   * |NGC0000001| 4.493667| -1.4414990980543227|    1|    1|
   * |NGC0000002| 3.787274|  1.3298379564211742|    2|    1|
   * |NGC0000003| 3.423602|-0.29457151504987844|    3|    1|
   * |NGC0000004|2.6619017|  1.3957536426732444|    4|    1|
   * +----------+---------+--------------------+-----+-----+
   * only showing top 5 rows
   *
   * }}}
   */
  implicit class FitsContext(spark : SparkSession) extends Serializable {

    // Initialise Hadoop configuration
    val conf = new Configuration(spark.sparkContext.hadoopConfiguration)

    // This will contain all options use to load the data
    private[sparkfits] val extraOptions = new scala.collection.mutable.HashMap[String, String]

    // This will contain the info about the schema
    // By default, the schema is inferred from the HDU header,
    // but the user can also manually specify the schema.
    private[sparkfits] var userSpecifiedSchema: Option[StructType] = None

    /**
      * Replace the current syntax in spark 2.X
      * spark.read.format("fits") --> spark.readfits
      * This is a hack to avoid touching DataFrameReader class, for which the
      * constructor is private... If you have a better idea, bug me!
      *
      * @return FitsContext
      */
    def readfits : FitsContext = FitsContext.this

    /**
      * Adds an input options for reading the underlying data source.
      *
      * In general you can set the following option(s):
      * - option("HDU", <Int>)
      * - option("datatype", <String>)
      * - option("printHDUHeader", <Boolean>)
      *
      * Note that values pass as Boolean, Long, or Double will be first
      * converted to String and then decoded later on.
      *
      * @param key : (String)
      *   Name of the option
      * @param value : (String)
      *   Value of the option.
      */
    def option(key: String, value: String) : FitsContext = {
      // Update options
      FitsContext.this.extraOptions += (key -> value)

      // Update the conf (redundant?)
      conf.set(key, value)

      // Return FitsContext
      FitsContext.this
    }

    /**
      * Adds an input options for reading the underlying data source.
      * (key, boolean)
      *
      * @param key : (String)
      *   Name of the option
      * @param value : (Boolean)
      *   Value of the option.
      */
    def option(key: String, value: Boolean): FitsContext = {
      option(key, value.toString)
    }

    /**
      * Adds an input options for reading the underlying data source.
      * (key, Long)
      *
      * @param key : (String)
      *   Name of the option
      * @param value : (Long)
      *   Value of the option.
      */
    def option(key: String, value: Long): FitsContext = {
      option(key, value.toString)
    }

    /**
      * Adds an input options for reading the underlying data source.
      * (key, Double)
      *
      * @param key : (String)
      *   Name of the option
      * @param value : (Double)
      *   Value of the option.
      */
    def option(key: String, value: Double): FitsContext = {
      option(key, value.toString)
    }

    /**
      * Adds a schema to our data. It will overwrite the inferred schema from
      * the HDU header. Useful if the header is corrupted.
      *
      * @param schema : (StructType)
      *   The schema for the data (`StructType(List(StructField))`)
      * @return return the FitsContext (to chain operations)
      */
    def schema(schema: StructType): FitsContext = {
      FitsContext.this.userSpecifiedSchema = Option(schema)
      FitsContext.this
    }

    /** Load a BinaryTableHDU data contained in one HDU as a DataFrame.
      * The schema of the DataFrame is directly inferred from the
      * header of the fits HDU.
      *
      * @param fn : (String)
      *  Path + filename of the fits file to be read
      * @return : DataFrame
      */
    def load(fn : String) : DataFrame = {

      // Check that you can read the data!
      val dataType = Try {
        extraOptions("datatype")
      }
      dataType match {
        case Success(value) => extraOptions("datatype")
        case Failure(e : NullPointerException) =>
          throw new NullPointerException(e.getMessage)
        case Failure(e : NoSuchElementException) =>
          throw new NoSuchElementException("""
          You did not specify the data type!
          Please choose one of the following:
            spark.readfits.option("datatype", "table")
            spark.readfits.option("datatype", "image")
            """)
        case Failure(_) => println("Unknown Exception")
      }

      // Check that the user specifies table
      val dataTypeTable = extraOptions("datatype").contains("table")
      dataTypeTable match {
        case true => extraOptions("datatype")
        case false => throw new AssertionError("""
          Currently only reading data from table is supported.
          Support for image data will be added later.
          Please use spark.readfits.option("datatype", "table")
          """)
      }

      // Check that the user specifies table
      val isIndexHDU = Try {
        extraOptions("HDU")
      }
      isIndexHDU match {
        case Success(value) => extraOptions("HDU")
        case Failure(e : NullPointerException) =>
          throw new NullPointerException(e.getMessage)
        case Failure(e : NoSuchElementException) =>
          throw new NoSuchElementException("""
          You need to specify the HDU to be read!
          spark.readfits.option("HDU", <Int>)
            """)
        case Failure(_) => println("Unknown Exception")
      }

      // Open the file
      val path = new org.apache.hadoop.fs.Path(fn)
      val fs = path.getFileSystem(conf)
      val indexHDU = conf.get("HDU").toInt
      val fB = new FitsBlock(path, conf, indexHDU)
      val header = fB.readHeader(fB.blockBoundaries._1)

      // Check the header if needed
      if (extraOptions.contains("printHDUHeader")) {
        if (extraOptions("printHDUHeader").toBoolean) {
          println(s"+------ HEADER (HDU=$indexHDU) ------+")
          header.foreach(println)
          println("+----------------------------+")
        }
      }

      val keys = fB.getHeaderKeywords(header)
      val keysHasXtension = keys.contains("XTENSION")
      keysHasXtension match {
        case true => keysHasXtension
        case false => throw new AssertionError(s"""
          Are you really trying to read a BINTABLE?
          Your header has no keywords called XTENSION.
          Check that the HDU number you want to
          access is correct (current = $indexHDU).
          """)
      }

      val headerStart = header(0).contains("BINTABLE")
      val headerStartElement = header(0)
      headerStart match {
        case true => headerStart
        case false => throw new AssertionError(s"""
          Are you really trying to read a BINTABLE? Your header says that
          the XTENSION is $headerStartElement
          """)
      }

      val headerEND = header.reverse(0).contains("END")
      headerEND match {
        case true => headerEND
        case false => throw new AssertionError("""
          There is a problem with your HEADER. It should end with END.
          Is it a standard header of size 2880 bytes? You should check it
          using the option spark.readfits.option("printHDUHeader", true).
          """)
      }

      // Get the schema. By default it is built from the header, but the user
      // can also specify it manually.
      val schema = userSpecifiedSchema.getOrElse(getSchema(fB))

      // We do not need the data on the driver at this point.
      // The executors will re-open it later on.
      fB.data.close()

      // Distribute the table data
      val rdd = spark.sparkContext.newAPIHadoopFile(
        fn,
        classOf[FitsFileInputFormat],
        classOf[LongWritable],
        classOf[List[List[_]]],
        conf)
          .flatMap(x => x._2)
          .map(x => Row.fromSeq(x))

      // Return DataFrame with Schema
      spark.createDataFrame(rdd, schema)
    }
  }
}
