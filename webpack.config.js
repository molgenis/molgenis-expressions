const path = require('path');

module.exports = {
  context: path.resolve(__dirname, 'js/target/scala-2.13/molgenis-expressions-opt'),
  mode: 'production',
  entry: {
    main: './main.js'
  },
  output: {
    library: {
      type: 'umd'
    }
  }
}
