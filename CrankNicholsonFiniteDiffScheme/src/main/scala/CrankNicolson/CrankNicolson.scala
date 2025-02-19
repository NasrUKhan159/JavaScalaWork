package CrankNicolson

// In this code, we solve the one-dimensional heat equation over a rod of length
//𝐿 for a total time 𝑇. The spatial and time domains are discretized into Nx and
// Nt points, respectively. We then construct the coefficient matrix A and iteratively
// solve for the temperature distribution using the Crank-Nicolson scheme. Make sure you
// have the Breeze library installed, as it provides useful functions for numerical linear
// algebra and plotting. You can add it to your project by including the following
// dependency in your build.sbt file:
// libraryDependencies += "org.scalanlp" %% "breeze" % "1.2"

import breeze.linalg._
import breeze.plot._

object CrankNicolson {
  def main(args: Array[String]): Unit = {
    val L = 1.0 // Length of the rod
    val T = 0.1 // Total time
    val Nx = 10 // Number of spatial points
    val Nt = 100 // Number of time points
    val alpha = 0.01 // Thermal diffusivity

    val dx = L / (Nx - 1) // Spatial step size
    val dt = T / Nt // Time step size
    val r = alpha * dt / (dx * dx)

    // Initialize temperature array
    val u = DenseMatrix.zeros[Double](Nx, Nt + 1)
    val x = linspace(0.0, L, Nx)

    // Initial condition: u(x, 0) = sin(pi * x / L)
    for (i <- 0 until Nx) {
      u(i, 0) = math.sin(math.Pi * x(i) / L)
    }

    // Construct the coefficient matrix
    val A = DenseMatrix.zeros[Double](Nx, Nx)
    for (i <- 1 until Nx - 1) {
      A(i, i - 1) = -r / 2
      A(i, i) = 1 + r
      A(i, i + 1) = -r / 2
    }
    A(0, 0) = 1
    A(Nx - 1, Nx - 1) = 1

    // Time-stepping loop
    for (n <- 0 until Nt) {
      val b = DenseVector.zeros[Double](Nx)
      for (i <- 1 until Nx - 1) {
        b(i) = r / 2 * u(i - 1, n) + (1 - r) * u(i, n) + r / 2 * u(i + 1, n)
      }
      b(0) = 0
      b(Nx - 1) = 0
      val u_next = A \ b
      for (i <- 0 until Nx) {
        u(i, n + 1) = u_next(i)
      }
    }

    // Plot the results
    val f = Figure()
    val p = f.subplot(0)
    for (n <- 0 until Nt + 1 by Nt / 10) {
      p += plot(x, u(::, n), name = f"t=${n * dt}%.2f s")
    }
    p.xlabel = "Position"
    p.ylabel = "Temperature"
    p.title = "Heat Equation: Crank-Nicolson Method"
    p.legend = true
    f.refresh()
  }
}