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
package org.koin.compiler.generator

val DEFAULT_MODULE_HEADER = """
        package org.koin.ksp.generated
    
        import org.koin.core.KoinApplication
        import org.koin.core.module.Module
        import org.koin.dsl.*
        
    """.trimIndent()

val DEFAULT_MODULE_FUNCTION = """
        public fun KoinApplication.defaultModule(): KoinApplication = modules(defaultModule)
        public val defaultModule : Module = module {
    """.trimIndent()

val DEFAULT_MODULE_FOOTER = """
    
        }
    """.trimIndent()

fun moduleHeader() = """
    package org.koin.ksp.generated
    
    import org.koin.core.module.Module
    import org.koin.dsl.*
    
""".trimIndent()

fun modulePartMethodHeader(moduleName: String, index: Int)="""
    public fun Module.${moduleName}${index}(){
""".trimIndent()
