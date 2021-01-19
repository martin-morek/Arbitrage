import $ivy.{`io.circe::circe-parser:0.12.3`}

import Graph._
import io.circe.parser

import scala.collection.mutable.{Map => mutableMap}
import scala.io.Source
import scala.math.log
import scala.util.Random

sealed trait Currency
case object USD extends Currency
case object EUR extends Currency
case object JPY extends Currency
case object BTC extends Currency

object Currency {
  def apply(currency: String): Currency = currency match {
    case "USD" => USD
    case "EUR" => EUR
    case "JPY" => JPY
    case "BTC" => BTC
  }
}

object Graph{
  type Distance = Double
  type Predecessor = Option[Currency]
  type Relaxation = Map[Currency, Cost]

  case class Edge(from: Currency, to: Currency, weight: Double)
  case class Vertex(id: Currency, edges: List[Edge])
  case class Cost(distance: Distance, predecessor: Predecessor)

  object Relaxation{
    def initialize(vertices: List[Vertex], source: Currency): Relaxation = {
      vertices.map{ v =>
        if(v.id == source) (v.id, Cost(0, None))
        else (v.id, Cost(Double.PositiveInfinity, None))
      }.toMap
    }
  }
}

object Arbitrage {
  def downloadRates(): Option[Map[String, Distance]] = {
    val sourceUrl = Source.fromURL("https://fx.priceonomics.com/v1/rates/")
    parser.decode[Map[String, Double]](sourceUrl.mkString).toOption
  }

  def ratesToVertices(rates: Map[String, Double]): List[Vertex] = {
    val edges = rates.keySet.map{key =>
      val (from, to): (String, String) = key.split('_').toSeq match { case Seq(a, b) => (a, b) }
      Edge(Currency(from), Currency(to), rates(key))
    }.toList

    edges.groupBy(_.from).map { case (id, edges) =>
      Vertex(id, edges)
    }.toList
  }

  def bellmanFord(vertices: List[Vertex], source: Currency): List[List[Currency]] = {
    val relaxation = mutableMap() ++ Relaxation.initialize(vertices, source)
    val edges = edgesToNegLog(vertices.flatten(_.edges))

    for(_ <- 0 until vertices.size - 1) {
      edges.foreach{ edge =>
        val possibleNewDistance = edge.weight + relaxation(edge.from).distance

        if(possibleNewDistance < relaxation(edge.to).distance)
          relaxation(edge.to) = Cost(possibleNewDistance, Some(edge.from))
      }
    }

    edges.map{ edge =>
      val possibleNewDistance = edge.weight + relaxation(edge.from).distance

      if(possibleNewDistance < relaxation(edge.to).distance) {
        retraceNegativeLoop(relaxation.toMap, source, List(source))
      } else List()
    }
  }

  def edgesToNegLog(edges: List[Edge]): List[Edge] =
    Random.shuffle(edges.map(e => e.copy(weight = -log(e.weight))))

  def retraceNegativeLoop(relaxedBF: Relaxation, source: Currency, acc: List[Currency]): List[Currency] = {
    relaxedBF(source).predecessor match {
      case Some(p) if acc.contains(p) => p +: acc.splitAt(acc.indexOf(p)+1)._1
      case Some(p) => retraceNegativeLoop(relaxedBF, p, p +: acc)
      case _ => acc
    }
  }

  def extractArbitragePath(edges: List[Edge], arbitrage: List[Currency]): List[Edge] =
    for ((from,to) <- arbitrage zip arbitrage.drop(1)) yield {
      edges.filter(e => e.from == from && e.to == to).head
    }

  def printRates(edges: List[Edge]) = {
    println("Conversion rates:")
    edges.sortBy(_.from.toString).foreach(e =>
      println(s"${e.from} -> ${e.to}: ${e.weight}")
    )
  }

  def printArbitrage(paht: List[Edge]) = {
    paht.foldLeft(100.0){ (acc, e: Edge) =>
      println(s"$acc ${e.from} * ${e.weight} = ${acc*e.weight} ${e.to}")
      acc * e.weight
    }
    println("---------------------------------------------------------")
  }

  def run(): Unit = {
    downloadRates() match {
      case Some(rates) =>
        val vertices = ratesToVertices(rates)
        val edges = vertices.flatten(_.edges)

        printRates(vertices.flatten(_.edges))

        val arbitrages = vertices.flatMap(vertex =>
          bellmanFord(vertices, vertex.id)
            .filter(_.nonEmpty).distinct)

        if(arbitrages.isEmpty) {
          println("\nNo arbitrage possibility found")
        } else {
          println("\nFound arbitrage possibilities: \n")
          arbitrages.distinct.foreach { arb =>
            val path = extractArbitragePath(edges, arb)
            printArbitrage(path)
          }
        }
      case None => println("Cannot download rates!")
    }
  }

}

Arbitrage.run()
