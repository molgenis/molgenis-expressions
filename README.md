# MOLGENIS expressions
[![Sonatype Nexus (Repository)](https://img.shields.io/nexus/maven-releases/org.molgenis/molgenis-expressions_2.13?server=https%3A%2F%2Fregistry.molgenis.org)](https://registry.molgenis.org/#browse/browse:maven-releases:org%2Fmolgenis%2Fmolgenis-expressions_2.13)
[![npm](https://img.shields.io/npm/v/@molgenis/expressions)](https://www.npmjs.com/package/@molgenis/expressions)

Library for evaluation of simple expressions given a context of variables.
Compatible with the expression language used in [Survey JS](https://github.com/surveyjs/survey-library/tree/master/src/expressions).
Works both in JavaScript and in the JVM.


Some examples:
* `age({dateOfBirth}) >= 18 and !{driverslicense}`
* `{reason} anyof ['No', 'Unsure']`
* `regex('^[1-9][0-9]{3}[\\s]?[A-Z]{2}$', {zipcode}, 'i')`

## operators

In decreasing order of precedence, the operators are:
### unary operators
|operator|meaning|
|--------|-------|
| `negate` / `!` | Boolean negation `!a` is `false` if a is [truthy](#truthy) |
|`empty`      | `a empty` if `a` is `[]`, `""` or `null`/`undefined`       |
|`notempty`   | `a notempty` if `a` is not `[]`, `""` or `null`/`undefined`|

### binary set operators
Can be applied to elements or lists of elements.

|operator|meaning|
|--------|-------|
|`contains`/`allof` | `a contains b` if all elements in `b` are also in `a` |
| `notcontains` | `a notcontains b` if at least one element in `b` is not in `a` |
| `anyof` | `a anyof b` if at least one element in `b` is also in `a` |

### arithmetic operators
With their usual meaning and precedence: `^`, `* / %`, `+ -`.

To achieve compatibility with JavaScript, all numerical values are converted to `Doubles` in the
JVM. The modulo operator `%` performs integer modulo division after converting the operands to `Long`
values.

### comparison operators
Compare numerical values. Can be denoted by symbol or name:
* `<=` /  `lessorequal`
* `<` / `less`
* `=` / `equal`
* `>=` / `greaterorequal`
* `>` / `greater`
* `!=` / `notequal`
### boolean operators
* `&&` / `and`
* `||` / `or`

## <a name="truthy"></a>truthiness
The boolean operators convert their operands to booleans based on their
'truthiness'. All values are truthy, except for:
* `false`
* `0`
* `""` (the empty string)
* `null`/`undefined`
* `NaN`

## functions
The following functions are available

### today
`today()` returns today's date, in the local timezone, in the form 'yyyy-mm-dd'

### currentYear
`currentYear()` returns the year of today's date, in the local timezone, as an integer number

### age
`age(dob, date)` returns the age given date of birth dob on a given date.
The parameters must be an yyyy-mm-dd ISO date string.

### regex
`regex(expression, string, flags)` tests a string against a regular expression,
returns true if it finds any matches.
Flags is a string of characters. Available flags:

| flag | meaning |
|---|---------|
| i | case insensitive |
| m | multiline |
| s | dotall |

## JavaScript
[![npm](https://img.shields.io/npm/v/@molgenis/expressions)](https://www.npmjs.com/package/@molgenis/expressions)

Check the [demo](https://codepen.io/fdlk/full/GRWQjOB).

The library is quite large (currently 334kB, 88kB zipped), because some of the Scala SDK gets
included.
It can be added using npm:
```
npm add @molgenis/expressions
```
In your code, import the module:
```javascript
import { Expressions } from '@molgenis/expressions'
```
To evaluate an expression:
```javascript
// returns 42
Expressions.evaluate("{foo}", {foo: 42})
```
It will throw an error if parsing or evaluation fails.

To parse and get a list of variable names:
```javascript
// returns ['foo']
Expressions.variableNames("{foo}")
```
## Java
[![Sonatype Nexus (Repository)](https://img.shields.io/nexus/maven-releases/org.molgenis/molgenis-expressions_2.13?server=https%3A%2F%2Fregistry.molgenis.org)](https://registry.molgenis.org/#browse/browse:maven-releases:org%2Fmolgenis%2Fmolgenis-expressions_2.13)

First add the library as a dependency.
### maven
Add the MOLGENIS nexus as a repository
```
<repositories>
  <repository>
    <id>molgenis-nexus</id>
    <name>Maven public group on registry.molgenis.org</name>
    <url>https://registry.molgenis.org/repository/maven-releases</url>
  </repository>
</repositories>
```
and include the library as a dependency
```
<dependency>
  <groupId>org.molgenis</groupId>
  <artifactId>molgenis-expressions_2.13</artifactId>
  <version>0.14.2</version>
</dependency>
```
### gradle (untested)
Add the MOLGENIS nexus as a repository
```
repositories {
  maven {
    url "https://registry.molgenis.org/repository/maven-releases"
  }
}
```
and include the library as a dependency
```
implementation 'org.molgenis:molgenis-expressions_2.13:0.14.2'
```
### evaluate expressions
In your code, create an Expressions instance.
The parameter is the number of parsed expressions that it should cache.
The library depends on Caffeine for the cache.

```java
val evaluator = new Expressions(1000);
val expressions = List.of("{foo} > {bar}", "{foo} + {bar}");
```
Optionally, retrieve the variable names for a list of expressions, so that
you know what variables to put in the context.
```java
// returns Set<String> containing ["foo", "bar"]
evaluator.getAllVariableNames(expressions);
```

Optionally, check a single expression to see if it can be parsed
```java
// throws ParseException
evaluator.getVariableNames("{foo");
// throws Set<String> containing ["foo", "bar]
evaluator.getVariableNames("{foo} > {bar}");
```

Parse and evaluate one or more expressions in a context:
```java
val context = Map.of("foo", 3, "bar", 2);
// returns List<Object> containing [true, 5.0]
evaluator.parseAndEvaluate(expressions, context);
```
