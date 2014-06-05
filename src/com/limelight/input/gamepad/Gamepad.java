package com.limelight.input.gamepad;

import java.util.Map;
import java.util.Map.Entry;

import com.limelight.LimeLog;
import com.limelight.input.Device;
import com.limelight.input.DeviceListener;
import com.limelight.input.gamepad.GamepadMapping.Mapping;
import com.limelight.input.gamepad.SourceComponent.Direction;
import com.limelight.input.gamepad.SourceComponent.Type;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.settings.GamepadSettingsManager;

/**
 * Represents a gamepad connected to the system
 * @author Diego Waxemberg
 */
public class Gamepad implements DeviceListener {

	private short inputMap = 0x0000;
	private byte leftTrigger = 0x00;
	private byte rightTrigger = 0x00;
	private short rightStickX = 0x0000;
	private short rightStickY = 0x0000;
	private short leftStickX = 0x0000;
	private short leftStickY = 0x0000;

	private NvConnection conn;

	public Gamepad(NvConnection conn) {
		this.conn = conn;
	}

	public Gamepad() {
		this(null);
	}

	public void setConnection(NvConnection conn) {
		this.conn = conn;
	}

	public void handleButtons(Device device, float[] buttonStates) {
		GamepadMapping mapping = GamepadSettingsManager.getSettings();

		for (int id = 0; id < buttonStates.length; id++) {
			Mapping mapped = mapping.get(new SourceComponent(Type.BUTTON, id, null));
			if (mapped == null) {
				//LimeLog.info("Unmapped button pressed: " + buttonId);
				continue;
			}

			if (!mapped.padComp.isAnalog()) {
				handleDigitalComponent(mapped, (int)buttonStates[id]);
			} else {
				handleAnalogComponent(mapped.padComp, sanitizeValue(mapped, buttonStates[id]));
			}

			//used for debugging
			//printInfo(device, new SourceComponent(Type.BUTTON, id, Direction.POSITIVE), mapped.padComp, buttonStates[id]);

		}
		device.setButtonsState(buttonStates);

	}

	public void handleAxes(Device device, float[] axesStates) {
		GamepadMapping mapping = GamepadSettingsManager.getSettings();

		Map<SourceComponent, Mapping> currentlyMapped = mapping.getAllMappings();

		for (Entry<SourceComponent, Mapping> entry : currentlyMapped.entrySet()) {
			int id = entry.getKey().getId();
			Mapping mapped = entry.getValue();

			float newValue = sanitizeValue(mapped, axesStates[id]);
			float lastValue = sanitizeValue(mapped, device.getAxisState(id));


			Direction mappedDir = null;
			if (newValue == 0) {
				if (lastValue >= 0) {
					mappedDir = Direction.POSITIVE;
				} else {
					mappedDir = Direction.NEGATIVE;
				}
			} else {
				mappedDir = newValue > 0 ? Direction.POSITIVE : Direction.NEGATIVE;
			}

			if (mappedDir != entry.getKey().getDirection()) {
				continue;
			}

			if (mapped.padComp.isAnalog()) {
				handleAnalogComponent(mapped.padComp, newValue);
			} else {
				handleDigitalComponent(mapped, Math.round(newValue));
			}

			//used for debugging
			//printInfo(device, new SourceComponent(Type.AXIS, id, mappedDir), mapped.padComp, newValue);
		}
		device.setAxesState(axesStates);
	}

	private float sanitizeValue(Mapping mapped, float value) {
		float retVal = value;
		if (mapped.invert) {
			retVal = -retVal;
		}
		if (mapped.trigger) {
			retVal = (retVal + 1) / 2;
		}
		return retVal;
	}

	private void handleAnalogComponent(GamepadComponent padComp, float value) {
		switch (padComp) {
		case LS_RIGHT:
			leftStickX = (short)Math.round(Math.abs(value) * 0x7FFF);
			break;
		case LS_LEFT:
			leftStickX = (short)Math.round(-Math.abs(value) * 0x7FFF);
			break;
		case LS_UP:
			leftStickY = (short)Math.round(Math.abs(value) * 0x7FFF);
			break;
		case LS_DOWN:
			leftStickY = (short)Math.round(-Math.abs(value) * 0x7FFF);
			break;
		case RS_UP:
			rightStickY = (short)Math.round(Math.abs(value) * 0x7FFF);
			break;
		case RS_DOWN:
			rightStickY = (short)Math.round(-Math.abs(value) * 0x7FFF);
			break;
		case RS_RIGHT:
			rightStickX = (short)Math.round(Math.abs(value) * 0x7FFF);
			break;
		case RS_LEFT:
			rightStickX = (short)Math.round(-Math.abs(value) * 0x7FFF);
			break;
		case LT:
			// HACK: Fix polling so we don't have to do this
			if (Math.abs(value) < 0.9) {
				value = 0;
			}
			leftTrigger = (byte)Math.round(Math.abs(value) * 0xFF);
			break;
		case RT:
			// HACK: Fix polling so we don't have to do this
			if (Math.abs(value) < 0.9) {
				value = 0;
			}
			rightTrigger = (byte)Math.round(Math.abs(value) * 0xFF);
			break;
		default:
			LimeLog.warning("A mapping error has occured. Ignoring: " + padComp.name());
			break;
		}

	}

	private void handleDigitalComponent(Mapping mapped, int pressed) {
		switch (mapped.padComp) {
		case BTN_A:
			toggle(ControllerPacket.A_FLAG, pressed);
			break;
		case BTN_X:
			toggle(ControllerPacket.X_FLAG, pressed);
			break;
		case BTN_Y:
			toggle(ControllerPacket.Y_FLAG, pressed);
			break;
		case BTN_B:
			toggle(ControllerPacket.B_FLAG, pressed);
			break;
		case DPAD_UP:
			toggle(ControllerPacket.UP_FLAG, pressed);
			break;
		case DPAD_DOWN:
			toggle(ControllerPacket.DOWN_FLAG, pressed);
			break;
		case DPAD_LEFT:
			toggle(ControllerPacket.LEFT_FLAG, pressed);
			break;
		case DPAD_RIGHT:
			toggle(ControllerPacket.RIGHT_FLAG, pressed);
			break;
		case LS_THUMB:
			toggle(ControllerPacket.LS_CLK_FLAG, pressed);
			break;
		case RS_THUMB:
			toggle(ControllerPacket.RS_CLK_FLAG, pressed);
			break;
		case LB:
			toggle(ControllerPacket.LB_FLAG, pressed);
			break;
		case RB:
			toggle(ControllerPacket.RB_FLAG, pressed);
			break;
		case BTN_START:
			toggle(ControllerPacket.PLAY_FLAG, pressed);
			break;
		case BTN_BACK:
			toggle(ControllerPacket.BACK_FLAG, pressed);
			break;
		case BTN_SPECIAL:
			toggle(ControllerPacket.SPECIAL_BUTTON_FLAG, pressed);
			break;
		default:
			LimeLog.warning("A mapping error has occured. Ignoring: " + mapped.padComp.name());
			return;
		}
	}

	/*
	 * Prints out the specified event information for the given gamepad
	 * used for debugging, normally unused.
	 */
	@SuppressWarnings("unused")
	private void printInfo(Device device, SourceComponent sourceComp, GamepadComponent padComp, float value) {

		StringBuilder builder = new StringBuilder();

		builder.append(sourceComp.getType().name() + ": ");
		builder.append(sourceComp.getId() + " ");
		builder.append("mapped to: " + padComp + " ");
		builder.append("changed to " + value);

		LimeLog.info(builder.toString());
	}

	/*
	 * Toggles a flag that indicates the specified button was pressed or released
	 */
	private void toggle(short button, int pressed) {
		if (pressed == 1) {
			inputMap |= button;
		} else {
			inputMap &= ~button;
		}
	}


	/*
	 * Sends a controller packet to the specified connection containing the current gamepad values
	 */
	public void fireControllerPacket() {
		if (conn != null) {
			conn.sendControllerInput(inputMap, leftTrigger, rightTrigger, 
					leftStickX, leftStickY, rightStickX, rightStickY);
		}
	}

}
