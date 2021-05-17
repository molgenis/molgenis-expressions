<template>
  <div>
    <h1>Evaluate demo</h1>
    <form>
      Expression:<input type="text" v-model="expression"/><br/>
      Data: <textarea v-model="data"/>
    </form>
    <pre> {{ result }} </pre>
  </div>
</template>

<script>
  import {evaluate} from "../../../target/scala-2.13/molgenis-expressions-fastopt/main.mjs"
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
          return evaluate(this.expression, JSON.parse(this.data))
        }
        catch (error) {
          return error
        }
      }
    }
  }
</script>