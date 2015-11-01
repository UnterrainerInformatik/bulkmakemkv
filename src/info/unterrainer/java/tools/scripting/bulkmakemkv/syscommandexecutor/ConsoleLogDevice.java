package info.unterrainer.java.tools.scripting.bulkmakemkv.syscommandexecutor;

public class ConsoleLogDevice implements LogDevice {

	@Override
	public void log(String str) {
		System.out.println(str);
	}
}
