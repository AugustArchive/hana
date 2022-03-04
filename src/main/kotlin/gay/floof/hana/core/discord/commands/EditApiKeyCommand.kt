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
import gay.floof.hana.core.database.tables.ApiKeysTable
import net.perfectdreams.discordinteraktions.common.builder.message.embed
import net.perfectdreams.discordinteraktions.common.commands.ApplicationCommandContext
import net.perfectdreams.discordinteraktions.common.commands.SlashCommandExecutor
import net.perfectdreams.discordinteraktions.common.commands.SlashCommandExecutorDeclaration
import net.perfectdreams.discordinteraktions.common.commands.options.ApplicationCommandOptions
import net.perfectdreams.discordinteraktions.common.commands.options.SlashCommandArguments
import org.jetbrains.exposed.sql.update

class EditApiKeyCommand: SlashCommandExecutor() {
    companion object: SlashCommandExecutorDeclaration(EditApiKeyCommand::class) {
        object Options: ApplicationCommandOptions() {
            val description = optionalString("description", "Edits your API key's description").register()
            val imEnabled = optionalBoolean("im", "Enables or disables the alpha image manipulation endpoints.").register()
            val nsfwEnabled = optionalBoolean("nsfw", "Enables or disables the NSFW endpoints.").register()
        }

        override val options: ApplicationCommandOptions = Options
    }

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

        // get the arguments
        val desc = args[Options.description]
        val nsfwEnabled = args[Options.nsfwEnabled]
        val imEnabled = args[Options.imEnabled]

        // Update the data accordingly
        asyncTransaction {
            ApiKeysTable.update({
                ApiKeysTable.id eq context.sender.id.value.toLong()
            }) {
                if (desc != null) {
                    it[description] = desc
                }

                val perms = mutableListOf<String>()
                val oldPerms = found!!.permissions?.split("|") ?: listOf()

                if (nsfwEnabled != null && nsfwEnabled == true) {
                    if (!oldPerms.contains("nsfw")) perms.add("nsfw")
                }

                if (imEnabled != null && imEnabled == true) {
                    if (!oldPerms.contains("im")) perms.add("im")
                }

                if (perms.isNotEmpty()) {
                    it[permissions] = perms.joinToString("|")
                }
            }
        }

        val newData = asyncTransaction {
            ApiKeyEntity.findById(context.sender.id.value.toLong())!!
        }

        context.sendEphemeralMessage {
            content = buildString {
                appendLine(":thumbsup: **| Successfully edited app \"${found!!.name}\"!**")
            }

            embed {
                color = Color(0x6AAFDC)
                this.description = buildString {
                    appendLine("• **Application Name**: ${newData.name}")
                    appendLine("• **Application Description**: ${newData.description}")

                    val nsfw = newData.permissions?.split("|")?.contains("nsfw") ?: false
                    val im = newData.permissions?.split("|")?.contains("im") ?: false

                    appendLine("• **NSFW Enabled**: ${if (nsfw) "Yes" else "No"}")
                    appendLine("• **(Alpha) Image Manipulation Enabled**: ${if (im) "Yes" else "No"}")
                }
            }
        }
    }
}
