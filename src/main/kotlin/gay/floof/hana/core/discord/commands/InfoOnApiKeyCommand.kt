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
import gay.floof.hana.core.database.asyncTransaction
import gay.floof.hana.core.database.tables.ApiKeyEntity
import net.perfectdreams.discordinteraktions.common.builder.message.embed
import net.perfectdreams.discordinteraktions.common.commands.ApplicationCommandContext
import net.perfectdreams.discordinteraktions.common.commands.SlashCommandExecutor
import net.perfectdreams.discordinteraktions.common.commands.SlashCommandExecutorDeclaration
import net.perfectdreams.discordinteraktions.common.commands.options.SlashCommandArguments

class InfoOnApiKeyCommand: SlashCommandExecutor() {
    companion object: SlashCommandExecutorDeclaration(InfoOnApiKeyCommand::class)

    override suspend fun execute(context: ApplicationCommandContext, args: SlashCommandArguments) {
        context.deferChannelMessageEphemerally()

        // Check if the user has an API key registered.
        val found = asyncTransaction {
            ApiKeyEntity.findById(context.sender.id.value.toLong())
        }

        if (found == null) {
            context.sendEphemeralMessage {
                content = ":thinking: **| You don't have any API keys registered. Use the `/create` slash command to register one!**"
            }
        }

        context.sendEphemeralMessage {
            embed {
                color = Color(0x6AAFDC)
                this.description = buildString {
                    appendLine("• **Application Name**: ${found!!.name}")
                    appendLine("• **Application Description**: ${found.description}")
                    appendLine("• **API Key**: ||${found.token}||")

                    val nsfw = found.permissions?.split("|")?.contains("nsfw") ?: false
                    val im = found.permissions?.split("|")?.contains("im") ?: false

                    appendLine("• **NSFW Enabled**: ${if (nsfw) "Yes" else "No"}")
                    appendLine("• **(Alpha) Image Manipulation Enabled**: ${if (im) "Yes" else "No"}")
                }
            }
        }
    }
}
