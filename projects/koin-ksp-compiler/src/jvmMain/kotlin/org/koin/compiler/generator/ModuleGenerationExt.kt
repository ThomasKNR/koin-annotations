/*
 * Copyright 2017-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.google.devtools.ksp.symbol.KSDeclaration
import org.koin.compiler.generator.*
import org.koin.compiler.metadata.KoinMetaData
import java.io.OutputStream

private val ROWS_PER_METHOD_LIMIT = 500

fun OutputStream.generateFieldDefaultModule(definitions: List<KoinMetaData.Definition>) {
    val standardDefinitions = definitions.filter { it.isNotScoped() }.toSet()
    val scopeDefinitions = definitions.filter { it.isScoped() }.toSet()

    standardDefinitions.forEach { generateDefaultModuleDefinition(it) }
    scopeDefinitions
        .groupBy { it.scope }
        .forEach { (scope, definitions) ->
            appendText(generateScope(scope!!))
            definitions.forEach { definition ->
                generateDefaultModuleDefinition(definition)
            }
            appendText(generateScopeClosing())
        }
}

fun OutputStream.generateDefaultModuleDefinition(definition: KoinMetaData.Definition) {
    if (definition is KoinMetaData.Definition.ClassDefinition){
        generateClassDeclarationDefinition(definition)
    } else if (definition is KoinMetaData.Definition.FunctionDefinition && !definition.isClassFunction) {
        generateFunctionDeclarationDefinition(definition)
    }
}

fun generateClassModule(classFile: OutputStream, module: KoinMetaData.Module) {
    classFile.appendText(moduleHeader())
    classFile.appendText(module.definitions.generateImports())

    val generatedField = module.generateModuleField(classFile)

    val modulePath = "${module.packageName}.${module.name}"
    val moduleName = "${module.packageName("_")}_${module.name}"

    module.includes?.let { includes ->
        if (includes.isNotEmpty()) {
            generateIncludes(includes, classFile)
        }
    }

    if (module.definitions.isNotEmpty()) {
        if (module.definitions.any {
                // if any definition is a class function, we need to instantiate the module instance
                // to able to call the function on this instance.
                it is KoinMetaData.Definition.FunctionDefinition &&
                        it.isClassFunction
            }) {
            classFile.appendText("${NEW_LINE}val moduleInstance = $modulePath()")
        }

        repeat(module.definitions.size / ROWS_PER_METHOD_LIMIT + 1) { index ->
            classFile.appendText("${NEW_LINE}${moduleName}${index}()")
        }
        classFile.appendText("\n}")

        generateDefinitions(module, moduleName, classFile)
    }

    classFile.appendText("\n}")
    val visibilityString = module.visibility.toSourceString()
    classFile.appendText(
        "\n${visibilityString}val $modulePath.module : org.koin.core.module.Module get() = $generatedField"
    )

    classFile.flush()
    classFile.close()
}

private fun generateDefinitions(
    module: KoinMetaData.Module,
    moduleName: String,
    classFile: OutputStream
) {
    var row = 0
    fun generateConfigHeader() {
        if (row % ROWS_PER_METHOD_LIMIT == 0) {
            if (row != 0) {
                classFile.appendText("\n}")
            }
            classFile.appendText("\n\n${modulePartMethodHeader(moduleName, row / ROWS_PER_METHOD_LIMIT)}")
        }
    }

    val standardDefinitions = module.definitions.filter { it.isNotScoped() }
    standardDefinitions.forEach {
        generateConfigHeader()
        row += 1
        it.generateTargetDefinition(classFile)
    }

    val scopeDefinitions = module.definitions.filter { it.isScoped() }
    scopeDefinitions
        .groupBy { it.scope }
        .forEach { (scope, definitions) ->
            generateConfigHeader()
            row += 1
            classFile.appendText(generateScope(scope!!))
            definitions.forEach {
                it.generateTargetDefinition(classFile)
            }
            // close scope
            classFile.appendText("\n\t\t\t\t}")
        }
}

private fun KoinMetaData.Definition.generateTargetDefinition(
    classFile: OutputStream
) {
    when (this) {
        is KoinMetaData.Definition.FunctionDefinition -> {
            if (isClassFunction) {
                classFile.generateModuleFunctionDeclarationDefinition(this)
            } else {
                classFile.generateFunctionDeclarationDefinition(this)
            }
        }

        is KoinMetaData.Definition.ClassDefinition -> classFile.generateClassDeclarationDefinition(
            this
        )
    }
}

private fun generateIncludes(
    includeList: List<KSDeclaration>,
    classFile: OutputStream
) {
    val generatedIncludes: String = includeList.generateModuleIncludes()
    classFile.appendText("${NEW_LINE}includes($generatedIncludes)")
}

private fun KoinMetaData.Module.generateModuleField(
    classFile: OutputStream
): String {
    val packageName = packageName("_")
    val generatedField = "${packageName}_${name}"
    val visibilityString = visibility.toSourceString()
    classFile.appendText("\n${visibilityString}val $generatedField : Module = module {")
    return generatedField
}

fun OutputStream.generateDefaultModuleHeader(definitions: List<KoinMetaData.Definition>) {
    appendText(DEFAULT_MODULE_HEADER)
    appendText(definitions.generateImports())
    appendText(DEFAULT_MODULE_FUNCTION)
}

fun OutputStream.generateDefaultModuleFooter() {
    appendText(DEFAULT_MODULE_FOOTER)
}

private fun List<KoinMetaData.Definition>.generateImports(): String {
    return mapNotNull { definition -> definition.keyword.import?.let { "import $it" } }
        .joinToString(separator = "\n", postfix = "\n")
}

private fun List<KSDeclaration>.generateModuleIncludes(): String {
    return joinToString { it.generateModuleInclude() }
}

private fun KSDeclaration.generateModuleInclude(): String {
    val packageName: String = packageName.asString()
    val className = simpleName.asString()
    return "$packageName.$className().module"
}
