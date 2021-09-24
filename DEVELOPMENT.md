# Development
The build setup contains a plugin that synchronizes between sbt and IntelliJ.
Jenkins builds the artifacts and does semantic release on them.

## Organisation
The sources are organized into projects.
### project
Basic build config
### shared
This is the bulk of the code.
The model classes for the expressions, the Parser and the Evaluator.
### jvm
Java-specific code. Here you'll find
* native implementations of date and regex functions
* a Java facade class that sits in front of all the Scala types and interfaces
* a [Caffeine](https://github.com/ben-manes/caffeine#readme) cache of parsed expressions
### js
JavaScript-specific code. Here you'll find
* native implementations of date and regex functions
* export annotations to create a javascript module

## Model
The model consists of simple case classes and case objects.
This makes it easy for the evaluator to match against.

## Parsing
The Parser is built using the [FastParse](https://com-lihaoyi.github.io/fastparse/) parser
combinators library.
Most of the grammar is pretty straightforward to parse, but the precedence of the binary operations
we use the [precedence climbing](https://en.wikipedia.org/wiki/Operator-precedence_parser) algorithm 
[suggested](https://twitter.com/li_haoyi/status/1004929982001393664?lang=en) by the author of
FastParse.

## Evaluation
The Evaluator evaluates the expressions against a context which is a
key-value map of variable values, and a map of functions.

Types are kept as simple as possible, taking a hint from JavaScript.
* Arithmetic operations assume everything is a double 
* Boolean operations use truthiness of the operands
* Set operations convert non-array arguments to singleton sets
* Comparisons pick up natural ordering from strings and numbers