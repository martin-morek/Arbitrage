# Arbitrage

This repository provides a solution for https://priceonomics.com/jobs/puzzle/ assignment in Scala language. 
The task it to find possible arbitrages in currency rates. Arbitrage is defined as near simultaneous purchase and sale 
of securities or foreign exchange in different markets in order to profit from price discrepancies.

## Solution analyse
There are several possible solutions for this kind of problem. Two mostly used are brute force and Bellman Ford 
algorithm. Brute force solution isn't very efficient since it requires to calculate all possible ratios between all 
currencies and because of this restriction my solution is based on Bellman Ford algorithm.

The [Bellman-Ford](https://en.wikipedia.org/wiki/Bellman–Ford_algorithm) algorithm is an algorithm that computes the 
shortest paths from a single source vertex to all of the other vertices in a weighted digraph. With small adjustments 
it can be applied to solve currency arbitrage. Currencies are represented as a graph nodes and edges weights are ratios 
between nodes. 

Arbitrage opportunities arise when a cycle is determined such that the edge weights satisfy the following expression
`w1 * w2 * w3 * … * wn > 1`. To be able to solve it with a graph we have to transform it to 
`(-log(w1)) + (-log(w2)) + (-log(w3)) + … + (-log(wn)) < 0`. It means that we transfer currency ratio (edge weight) to 
negative log() and apply Bellman-Ford algorithm. Currency rates offer arbitrage if graph contains a negative cycle.

### Time Complexity
Time complexity for is Bellman-Ford Algorithm: O(EV)

The graph representing relationship between currencies are dense graph so E = O(V * V). Which means that overall time 
complexity is O(V3), 
where:
 - V - total number of vertices in the graph (number of different kinds of currencies)
 - E - total number of edges in the graph


## Building
Project has to be run with [Ammonite](http://ammonite.io/#ScalaScript)

`amm Arbitrage.sc`