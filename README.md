# FlowChart Partial Evaluator

## Structure 
FlowChart programs:
- [turing-machine-int.fchart](fchart/turing-machine/turing-machine-int.fchart) -- interpreter for the Turing Machine
- [mix.fchart](fchart/mix.fchart) -- implementation of mix in FlowChart
- [01-example.tm](fchart/turing-machine/01-example.tm) -- example program for Turing Machine
  - [01-example.inp](fchart/turing-machine/01-example.inp) -- the same program, but as input for the interpreter
- [dict.fchart](fchart/dict.fchart) -- example program in FlowChart

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

It will evaluate mix on Turing Machine interpreter and an example program and output the results to the `compiled-1.fchart` file. 

## Second Futamura Projection

Similarly, run 
```
./gradlew secondProjection
```

It first runs mix on itself plus the interpreter, saving the compiled interpreter program to `compiler.fchart`.
Then it executes the compiler on the same example TM program and outputs the results to `compiled-2.fchart`.

The same programs can be found in the [reference directory](fchart/reference). 