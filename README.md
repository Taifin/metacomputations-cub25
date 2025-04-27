# FlowChart Partial Evaluator

## Structure 
FlowChart programs:
- [turing-machine-int.fchart](fchart/turing-machine/turing-machine-int.fchart) — interpreter for the Turing Machine
- [mix.fchart](fchart/mix.fchart) — implementation of mix in FlowChart
- [01-example.tm](fchart/turing-machine/01-example.tm) — example program for Turing Machine
  - [01-example.inp](fchart/turing-machine/01-example.inp) — the same program, but as input for the interpreter
- [dict.fchart](fchart/dict.fchart) — example program in FlowChart
- the [reference](fchart/reference) directory contains output programs for all three Futamura projections

## Interpreter 

To run the interpreter, execute:
```
./gradlew run --args=<program.fchart> < <input>
```

For example, to run TM interpreter on example program:
```
./gradlew run --args="fchart/turing-machine/turing-machine-int.fchart" < fchart/turing-machine/01-example.inp
```

### General rules 
Interpreter expects a file to interpret as an argument and then attempts to read variables declared in `read` block
from stdin. Valid input for `read` is any expression in FlowChart, including builtins

## First Futamura Projection

To check the first Futamura projection, run

```
./gradlew firstProjection
```

It will evaluate mix on Turing Machine interpreter and an example program and output the results to the `output/firstProjection/compiled.fchart` file. 

## Second Futamura Projection

Similarly, run 
```
./gradlew secondProjection
```

It first runs mix on itself plus the interpreter, saving the compiled interpreter program to `output/secondProjection/compiler.fchart`.
Then it executes the compiler on the same example TM program and outputs the results to `output/secondProjection/compiled.fchart`.

## Third Projection

```
./gradlew thirdProjection
```

First, it runs mix on mix and mix, producing compiler generator in `output/thirdProjection/compiler-gen.fchart`.
The compiler generator is then executed on TM interpreter to produce a compiler -- `output/thirdProjection/compiler.fchart`. 
This compiler is then evaluated on the same example TM program as above and produces a compiled program `output/thirdProjection/compiled.fchart`.

### Note 

Trivial live-variable analysis that is suitable for the second projection and produces the smallest `poly` set
(26 labels) is not fit for the third projection, 
as it erases one of the critical paths of compiler-generator and makes it stuck in the loop. 

Thus, the third projection uses a proper textbook version of the analysis, which produces a slightly higher `poly` -- 
the compiler generated that way has around 40 labels. 

The use of full analysis can be enforced with `-L`/`--full-live-vars` argument of interpreter. 