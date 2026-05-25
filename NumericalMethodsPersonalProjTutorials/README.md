### Numerical Methods Personal Project Tutorials using Scala

#### Tutorial 1: Explore the Huang method in ABS algorithms

The Huang Method, positioned within the ABS (Abaffy-Broyden-Spedicato) family of algorithms, is widely regarded as one of the most effective direct methods for solving linear systems of equations (Ax = b), especially when dealing with ill-conditioned systems. It systematically generates a sequence of search directions and projection matrices to determine a solution or detect system inconsistency. This method operates on general linear system Ax = b where A is an (mxn) real-valued matrix, x is an n-dimensional real-valued vector. It falls under the ABS class by managing a deflection/projection matrix $H_i$ initialized to the identity matrix $I$. The methodology of Huang method is:

For each iteration $i$ (from 1 to $m$):

\begin{enumerate}
    \item \textbf{Compute Residue:} Determine the constraint step using the current row of $A$ (denoted as $a_i^T$) and the projection matrix:
    \begin{equation}
        p_i = H_i a_i
    \end{equation}
    
    \item \textbf{Update Solution:} If $p_i \neq 0$, update the solution estimate:
    \begin{equation}
        x_{i+1} = x_i + \frac{b_i - a_i^T x_i}{a_i^T p_i} p_i
    \end{equation}
    
    \item \textbf{Update Deflection Matrix:} Adjust $H$ to project out the component along the current constraint:
    \begin{equation}
        H_{i+1} = H_i - \frac{H_i a_i a_i^T H_i}{a_i^T H_i a_i}
    \end{equation}
    \textit{(In the standard Huang algorithm, $H_i$ is symmetric, which optimizes memory and guarantees numerical resilience against round-off errors).}
\end{enumerate}

Code implementation:
- Numerical Stability: The denominator condition check (Math.abs(denominator) < 1e-12) prevents critical runtime division-by-zero errors when encountering linearly dependent rows.
- Scala Paradigms: The math constructs make frequent use of Array.tabulate and structural combinators like .zip and .map, providing a fast, cache-friendly implementation on the JVM.

Extensions:
- Test Huang method against specific highly ill-conditioned matrices e.g. Hilbert matrix.
- Implement other variations of the ABS Family (like the Implicit LX algorithm).

Scala file/s: HuangMethod.scala

#### Tutorial 2: Huang's generalized family of quasi-Newton updating formulas in Scala

In numerical analysis and optimization, "Huang's family of methods" primarily refers to a broad, multiparameter class of Quasi-Newton update formulas introduced by H.Y. Huang in 1970. It serves as a foundational theoretical framework for unconstrained non-linear optimization. 

When optimizing a function $f(x)$, quasi-Newton methods iteratively approximate the inverse Hessian matrix $H^{-1}$ using a matrix $B_{k}$. Huang generalized this process by creating a family of updates with three degrees of freedom (free parameters).

The general formula updates the approximate inverse matrix via:

$B_{k+1} = \rho_{k}B_{k} + \frac{s_{k}\gamma_{k}^{T}}{\gamma_{k}^{T}s_{k}} - \frac{\rho_{k}B_{k}\gamma_{k}\theta_{k}^{T}}{\gamma_{k}^{T}B_{k}\gamma_{k}}$ where $s_{k} = x_{k+1} - x_{k}$ is the step difference, $\gamma_{k}$ is the gradient change, and $\rho_{k}, \theta_{k}$ represent params chosen to dictate specific behaviours.

Key features and mathematical traits:

- Huang Secant Condition: Unlike standard quasi-Newton updates that strictly adhere to the classical secant equation $B_{k+1}\gamma_{k} = s_{k}$, Huang's family satisfies a scaled version: $B_{k+1}\gamma_{k} = \rho_{k}s_{k}$.
- Bypassing symmetry: The family explicitly includes updates where $B_{k}$ does not have to remain symmetric or positive-definite.
- Quadratic Termination: Under exact line searches, all algorithms falling within the Huang family generate identical search directions and converge to the minimum of a quadratic function in at most n steps.
- Famous Subsets: By restricting the parameters to enforce symmetry $\rho_{k} = 1$, the Huang family collapses into the well-known Broyden Family. This subset includes the widely used BFGS and DFP formulas.

Extensions:
- Extend this into a complete line-search optimization loop for a specific objective function.

Scala file/s: HuangOptimization.scala

#### References:
1. "Numerical Methods using Java: For Data Science, Analysis and Engineering" by Haksun Li 
2. Esmaeili, Hamid. "A numerical experiment with Huang algorithm." Boletín de matemáticas 14.1 (2007): 1-13.
3. https://indrag49.github.io/Numerical-Optimization/quasi-newton-methods.html
4. Odland, Tove. On Methods for Solving Symmetric Systems of Linear Equations Arising in Optimization. Diss. KTH Royal Institute of Technology, 2015.
5. Oren, So S. "On quasi-Newton and pseudo-Newton algorithms." Journal of Optimization Theory and Applications 20.2 (1976): 155-170.