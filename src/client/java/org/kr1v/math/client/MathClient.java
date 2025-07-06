package org.kr1v.math.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.*;


public class MathClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("mat")
				.then(ClientCommandManager.argument("expression", StringArgumentType.greedyString())
						.executes(commandContext -> {
							String[] resultS = handleMat(commandContext);
							if (resultS.length != 1) {
								return 0;
							}
							commandContext.getSource().sendFeedback(Text.literal(StringArgumentType.getString(commandContext, "expression") + " ="));
							commandContext.getSource().sendFeedback(Text.literal(String.join("", resultS)));
							return 1;
						}))));
		EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("mats")
				.then(ClientCommandManager.argument("expression", StringArgumentType.greedyString())
						.executes(commandContext -> {
							String[] resultS = handleMat(commandContext);
							if (resultS.length != 1) {
								return 0;
							}
							double result = Double.parseDouble(resultS[0]);
							commandContext.getSource().sendFeedback(Text.literal(StringArgumentType.getString(commandContext, "expression") + " ="));
							if (result >= 64) {
								long stacks = (long)(result)/64;
								long left = (long)(result)%64;
								commandContext.getSource().sendFeedback(Text.literal("%ss%s".formatted(stacks, left)));
							} else {
								commandContext.getSource().sendFeedback(Text.literal("%s".formatted(result)));
							}
							return 1;
						}))));
		EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("matc")
				.then(ClientCommandManager.argument("expression", StringArgumentType.greedyString())
						.executes(commandContext -> {
							String[] resultS = handleMat(commandContext);
							if (resultS.length != 1) {
								return 0;
							}
							commandContext.getSource().sendFeedback(Text.literal(StringArgumentType.getString(commandContext, "expression") + " ="));
							commandContext.getSource().sendFeedback(Text.literal(String.join("", resultS)));
							MinecraftClient.getInstance().keyboard.setClipboard(String.join("", resultS));
							return 1;
						}))));
	}
	private String[] handleMat(CommandContext<FabricClientCommandSource> commandContext) {
		String exp = StringArgumentType.getString(commandContext, "expression");
		exp = exp.replaceAll("\\s+", "");
		String[] components = exp.split("(?<=[*+\\-/()^s])|(?=[*+\\-/()^s])");
		try {
			components = calculate(components);
		} catch (IllegalArgumentException e){
			commandContext.getSource().sendFeedback(Text.literal(e.getMessage()));
			return components;
		} catch (Exception e) {
			commandContext.getSource().sendFeedback(Text.literal("Something went wrong!"));
			return components;
		}
		return components;
	}

	private String[] calculate(String[] components) throws Exception {
		double result = Double.parseDouble(reduceMathExpression(components)[0]);
		String[] temp = new String[1];
		if (Math.round(result) == result) {
			temp[0] = Long.toString((long)result);
		} else {
			temp[0] = Double.toString(result);
		}
		return temp;
	}

	private String[] reduceMathExpression(String[] components) throws Exception {
		if (components[0].equals("(")) {
			components = removeAt(components, 0);
			for (int i = 0; i < components.length; i++) {
				System.out.println(Arrays.toString(components));
				if (components[i].equals("(")) {
					components = ArrayUtils.addAll(Arrays.copyOfRange(components, 0, i), reduceMathExpression(Arrays.copyOfRange(components, i+1, components.length)));
					i = 0;
				}
				if (components[i].equals(")")) {
					components = removeAt(components, i);
				}
			}
		}
		if (components.length == 1) return components;
		boolean done = false;
		int loops = 0;
		while (!done) {
			for (int i = 0; i < components.length; i++) {
				System.out.println(Arrays.toString(components));
				if (components[i].equals(")")) {
					components = removeAt(components, i);
					i = 0;
					continue;
				}
				if (components[i+1].equals("s")) {
					double stacks = 0;
					double left = 0;
					try { stacks = Double.parseDouble(components[i]);
					} catch (Exception ignored) {}

					try { left = Double.parseDouble(components[i+2]);
					} catch (Exception ignored) {}
					components[i] = Double.toString(stacks*64 + left);
					components = removeAt(components, i+1); // removes the s
					components = removeAt(components, i+1); // removes left
					i = 0;
					continue;
				}
				if (i != 0 && i != components.length - 1) {
					if (components[i+1].equals("(")) {
						components = ArrayUtils.addAll(Arrays.copyOfRange(components, 0, i+1), reduceMathExpression(Arrays.copyOfRange(components, i+1, components.length)));
						i = 0;
						continue;
					}
					boolean hasExp = Arrays.asList(components).contains("^");

					if (hasExp && (components[i].equals("*") || components[i].equals("+") || components[i].equals("-") || components[i].equals("/"))) continue;

					boolean hasMulOrDiv = Arrays.stream(components).anyMatch(s -> s.equals("*") || s.equals("/"));

					if (hasMulOrDiv && (components[i].equals("+") || components[i].equals("-"))) continue;
					double result = getResult(components, i);

					if (components[i].equals("*") || components[i].equals("+") || components[i].equals("-") || components[i].equals("/") || components[i].equals("^")) {
						components[i - 1] = Double.toString(result);
						components = removeAt(components, i);
						components = removeAt(components, i);
					}
				}
			}
			if (components.length == 1) {
				done = true;
			}
			if (loops > 10000) {
				throw new Exception("Something went wrong!");
			}
			loops++;
		}
		return components;
	}

	private static double getResult(String[] components, int i) {
		double x = 0;
		double y = 0;
		double result = 0;
		if (components[i].equals("*") || components[i].equals("+") || components[i].equals("-") || components[i].equals("/") || components[i].equals("^")) {
			x = Double.parseDouble(components[i -1]);
			y = Double.parseDouble(components[i +1]);
		}
		if (components[i].equals("*")) result = x*y;
		if (components[i].equals("+")) result = x+y;
		if (components[i].equals("-")) result = x-y;
		if (components[i].equals("/")) result = x/y;
		if (components[i].equals("^")) result = Math.pow(x, y);
		return result;
	}

	private static String[] removeAt(String[] array, int index) {
		if (index < 0 || index >= array.length) {
			throw new IndexOutOfBoundsException();
		}

		String[] result = new String[array.length - 1];
		for (int i = 0, j = 0; i < array.length; i++) {
			if (i != index) {
				result[j++] = array[i];
			}
		}
		return result;
	}
}
