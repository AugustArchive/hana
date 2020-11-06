package dev.floofy.api.modules

import dev.floofy.api.data.Application
import dev.floofy.api.data.Config
import com.charleskorn.kaml.Yaml
import org.koin.dsl.module
import org.koin.dsl.bind

import java.io.File

val configModule = module {
    single {
        val file = File("config.yml")
        Yaml.default.decodeFromString(Config.serializer(), file.readText())
    }

    single {

    }
}
