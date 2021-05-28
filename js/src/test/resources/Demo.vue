<template>
  <div>
    <h1>Evaluate demo</h1>
    <form>
      <h2>Expression</h2>
      <input type="text" v-model="expression"/><br/>
      <h2>Variables</h2>
      <ul>
        <li v-for="variable in variables" :key="variable"><pre>{{variable}}</pre></li>
      </ul>
      <h2>Data</h2>
      <textarea v-model="data" rows="4"/>
    </form>
  
    <h2>Result</h2>
    <pre> {{ result }} </pre>
  </div>
</template>

<script>
  import {evaluate, variableNames} from "../../../target/scala-2.13/molgenis-expressions-opt/main.mjs"
  window.evaluate = evaluate
  export default {
    data () {
      return {
        expression: "{foo} > 12",
        data: '{\n  "foo": 15\n}'
      }
    },
    computed: {
      result () {
        try {
          return eval(`evaluate('${this.expression.replaceAll("'", "''")}',${this.data})`)
        }
        catch (error) {
          return error
        }
      },
      variables () {
        try {
          return variableNames(this.expression)
        }
        catch (error) {
          return []
        }
      }
    }
  }
</script>