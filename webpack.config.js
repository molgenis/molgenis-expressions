const path = require('path');

module.exports = {
  context: path.resolve(__dirname, 'js/target/scala-2.13/molgenis-expressions-opt'),
  mode: 'production',
  entry: {
    main: './main.js'
  },
  output: {
    environment: {
      // The environment supports arrow functions ('() => { ... }').
      arrowFunction: false,
      // The environment supports BigInt as literal (123n).
      bigIntLiteral: false,
      // The environment supports const and let for variable declarations.
      const: true,
      // The environment supports destructuring ('{ a, b } = obj').
      destructuring: false,
      // The environment supports an async import() function to import EcmaScript modules.
      dynamicImport: false,
      // The environment supports 'for of' iteration ('for (const x of array) { ... }').
      forOf: false,
      // The environment supports ECMAScript Module syntax to import ECMAScript modules (import ... from '...').
      module: false,
    },
    library: {
      name: 'Expressions',
      type: 'umd'
    }
  }
}
