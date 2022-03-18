/*
 * 🥀 hana: API to proxy different APIs like GitHub Sponsors, source code for api.floofy.dev
 * Copyright (c) 2020-2022 Noel <cutie@floofy.dev>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package gay.floof.hana.core.discord.commands

import dev.kord.common.Color
import gay.floof.hana.core.managers.JwtManager
import net.perfectdreams.discordinteraktions.common.builder.message.embed
import net.perfectdreams.discordinteraktions.common.commands.ApplicationCommandContext
import net.perfectdreams.discordinteraktions.common.commands.SlashCommandExecutor
import net.perfectdreams.discordinteraktions.common.commands.SlashCommandExecutorDeclaration
import net.perfectdreams.discordinteraktions.common.commands.options.ApplicationCommandOptions
import net.perfectdreams.discordinteraktions.common.commands.options.SlashCommandArguments

class CreateApiKeyCommand(private val jwt: JwtManager): SlashCommandExecutor() {
    companion object: SlashCommandExecutorDeclaration(CreateApiKeyCommand::class) {
        object Options: ApplicationCommandOptions() {
            val name = string("name", "The application name.").register()
            val description = optionalString("description", "The application's description").register()
            val nsfwEnabled = optionalBoolean("nsfw_enabled", "If any NSFW endpoints should be enabled. You must be 18 years of age or older to use this.").register()
            val imageManipulationEnabled = optionalBoolean("im_enabled", "If image manipulation should be enabled for this API key.").register()
        }

        override val options: ApplicationCommandOptions = Options
    }

    override suspend fun execute(context: ApplicationCommandContext, args: SlashCommandArguments) {
        context.deferChannelMessageEphemerally()

        // Grab the arguments
        val name = args[Options.name]
        val description = args[Options.description] ?: "this app doesn't have a description"
        val nsfwEnabled = args[Options.nsfwEnabled] ?: false
        val imEnabled = args[Options.imageManipulationEnabled] ?: false

        try {
            val token = jwt.create(context.sender.id.value.toLong(), name, description, nsfwEnabled, imEnabled)
            context.sendEphemeralMessage {
                content = buildString {
                    appendLine(":thumbsup: **Application \"$name\" has been created into the database!**")
                    appendLine("> The embed is basically what you filled out. If you want to edit anything,")
                    appendLine("> you can use the `/edit` command!")
                    appendLine()

                    if (nsfwEnabled) {
                        appendLine("> ")
                        appendLine("> :warning: You have enabled the NSFW endpoints: `/yiff`, `/yiff/random`, by enabling this,")
                        appendLine("> you are saying that you are 18 years or older to use these endpoints. **Noel** or the **Noelware** team")
                        appendLine("> is not responsible if you're underage and using these endpoints, since we do not want to add security risks")
                        appendLine("> of verifying user identification descriptors.")
                    }

                    if (imEnabled) {
                        appendLine("> ")
                        appendLine("> :warning: You have enabled the **alpha** image manipulation endpoints to do image editing.")
                        appendLine("> By enabling this, you are agreeing for more stricter ratelimits (since these types of endpoints use a lot of CPU and memory usage)")
                        appendLine("> and can take a while to generate if using huge data.")
                    }
                }

                embed {
                    color = Color(0x6AAFDC)
                    this.description = buildString {
                        appendLine("• **Application Name**: $name")
                        appendLine("• **Application Description**: $description")
                        appendLine("• **API Key**: ||$token||")
                        appendLine("• **NSFW Enabled**: ${if (nsfwEnabled) "Yes" else "No"}")
                        appendLine("• **(Alpha) Image Manipulation Enabled**: ${if (imEnabled) "Yes" else "No"}")
                    }
                }
            }
        } catch (e: Exception) {
            context.sendEphemeralMessage {
                content = "An unknown exception has occurred while generating your API key. Try again later!"
            }
        }
    }
}
