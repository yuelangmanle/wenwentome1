package com.wenwentome.reader.bridge.source

import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject

class RhinoScriptEvaluator {
    fun eval(script: String, bindings: Map<String, Any?>): String {
        val context = Context.enter().apply {
            optimizationLevel = -1
        }
        return try {
            val scope = context.initSafeStandardObjects()
            bindings.forEach { (key, value) ->
                ScriptableObject.putProperty(scope, key, value)
            }
            Context.toString(context.evaluateString(scope, script, "rule.js", 1, null))
        } finally {
            Context.exit()
        }
    }
}
