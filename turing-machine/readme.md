# Usage

To convert a list of instructions for TM to interpreter-readable format, run 
```
kotlinc -script turing-machine/tokenize.kts -- <program> <initial-tape>
```

e.g.

```
kotlinc -script turing-machine/tokenize.kts -- turing-machine/01-example.tm 110101
```

It will produce a file called `<program>.inp` that can be passed to `turing-machine-int.fchart` as an input.