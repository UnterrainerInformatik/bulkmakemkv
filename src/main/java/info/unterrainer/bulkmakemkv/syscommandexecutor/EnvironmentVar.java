package info.unterrainer.bulkmakemkv.syscommandexecutor;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(fluent = true)
class EnvironmentVar {

	private String name;
	private String value;
}