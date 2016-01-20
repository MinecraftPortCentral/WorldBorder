package com.wimbli.WorldBorder.cmd;

import static com.wimbli.WorldBorder.cmd.WBCmd.C_DESC;
import static com.wimbli.WorldBorder.cmd.WBCmd.commandEmphasized;

import com.wimbli.WorldBorder.Config;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

public class CmdBypass implements CommandExecutor {

    public static Text helpText =
            Text.builder().append(Text.of("If [player] isn't specified, command sender is used. If [on|off] isn't specified, the value will " +
                    "be toggled. Once bypass is enabled, the player will not be stopped by any borders until bypass is " +
                    "disabled for them again. Use the ")).append(commandEmphasized("bypasslist")).append(C_DESC)
                    .append(Text.of("command to list all " +
                            "players with bypass enabled.")).build();

    public CmdBypass() {
        //addCmdExample(nameEmphasized() + "{player} [on|off] - let player go beyond border.");
    }

	/*@Override
	public void cmdStatus(CommandSender sender)
	{
		if (!(sender instanceof Player))
			return;

		boolean bypass = Config.isPlayerBypassing(((Player)sender).getUniqueId());
		sender.sendMessage(C_HEAD + "Border bypass is currently " + enabledColored(bypass) + C_HEAD + " for you.");
	}*/

    public CommandResult execute(CommandSource commandSource, CommandContext commandContext) throws CommandException {
        Player player = commandContext.<Player>getOne("player").get();

        boolean bypassing = !Config.isPlayerBypassing(player.getUniqueId());
        if (commandContext.<Boolean>getOne("bypass").isPresent()) {
            bypassing = commandContext.<Boolean>getOne("bypass").get();
        }

        Config.setPlayerBypass(player.getUniqueId(), bypassing);

        player.sendMessage(
                Text.builder().append(Text.of("Border bypass is now ")).append(WBCmd.enabledColored(bypassing)).append(Text.of(".")).build());

        Config.log("Border bypass for player \"" + player.getName() + "\" is " + (bypassing ? "enabled" : "disabled") +
                (player != null ? " at the command of player \"" + player.getName() + "\"" : "") + ".");

        return CommandResult.success();
    }
}
