/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.bootstrap.encrypt;

import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.bootstrap.TextEncryptorBindHandler;
import org.springframework.cloud.bootstrap.TextEncryptorConfigBootstrapper;
import org.springframework.cloud.context.encrypt.EncryptorFactory;
import org.springframework.cloud.util.PropertyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;
import org.springframework.security.rsa.crypto.RsaSecretEncryptor;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public abstract class TextEncryptorUtils {

	/**
	 * Decrypt environment. See {@link DecryptEnvironmentPostProcessor}.
	 * @param decryptor the {@link AbstractEnvironmentDecrypt}
	 * @param environment the environment to get key properties from.
	 * @param propertySources the property sources to decrypt.
	 * @return the decrypted properties.
	 */
	// 解密环境。参见 {@link DecryptEnvironmentPostProcessor}。
	// @param decoder {@link AbstractEnvironmentDecrypt}
	// @param environment 要从中获取密钥属性的环境。
	// @param propertySources 要解密的属性源。
	// @return 解密后的属性。
	static Map<String, Object> decrypt(AbstractEnvironmentDecrypt decryptor, ConfigurableEnvironment environment,
			MutablePropertySources propertySources) {
		TextEncryptor encryptor = getTextEncryptor(decryptor, environment);
		return decryptor.decrypt(encryptor, propertySources);
	}

	static TextEncryptor getTextEncryptor(AbstractEnvironmentDecrypt decryptor, ConfigurableEnvironment environment) {
		Binder binder = Binder.get(environment);
		KeyProperties keyProperties = binder.bind(KeyProperties.PREFIX, KeyProperties.class)
			.orElseGet(KeyProperties::new);
		if (TextEncryptorUtils.keysConfigured(keyProperties)) {
			decryptor.setFailOnError(keyProperties.isFailOnError());
			if (ClassUtils.isPresent("org.springframework.security.rsa.crypto.RsaSecretEncryptor", null)) {
				RsaProperties rsaProperties = binder.bind(RsaProperties.PREFIX, RsaProperties.class)
					.orElseGet(RsaProperties::new);
				return TextEncryptorUtils.createTextEncryptor(keyProperties, rsaProperties);
			}
			return new EncryptorFactory(keyProperties.getSalt()).create(keyProperties.getKey());
		}
		// no keys configured
		return new TextEncryptorUtils.FailsafeTextEncryptor();
	}

	/**
	 * Register all classes that need a {@link TextEncryptor} in
	 * {@link TextEncryptorConfigBootstrapper}.
	 * @param registry the BootstrapRegistry.
	 */
	// 在 {@link TextEncryptorConfigBootstrapper} 中注册所有需要 {@link TextEncryptor} 的类。
	// @param registry BootstrapRegistry。
	public static void register(BootstrapRegistry registry) {
		registry.registerIfAbsent(TextEncryptor.class, context -> {
			KeyProperties keyProperties = context.get(KeyProperties.class);
			if (TextEncryptorConfigBootstrapper.keysConfigured(keyProperties)) {
				if (TextEncryptorConfigBootstrapper.RSA_IS_PRESENT) {
					RsaProperties rsaProperties = context.get(RsaProperties.class);
					return createTextEncryptor(keyProperties, rsaProperties);
				}
				return new EncryptorFactory(keyProperties.getSalt()).create(keyProperties.getKey());
			}
			// no keys configured
			return new FailsafeTextEncryptor();
		});
		registry.registerIfAbsent(BindHandler.class, context -> {
			TextEncryptor textEncryptor = context.get(TextEncryptor.class);
			if (textEncryptor != null) {
				KeyProperties keyProperties = context.get(KeyProperties.class);
				return new TextEncryptorBindHandler(textEncryptor, keyProperties);
			}
			return null;
		});
	}

	/**
	 * Promote the {@link TextEncryptor} to the {@link ApplicationContext}.
	 * @param bootstrapContext the Context.
	 * @param beanFactory the bean factory.
	 */
	// 将 {@link TextEncryptor} 提升到 {@link ApplicationContext}。
	// @param bootstrapContext 上下文。
	// @param beanFactory Bean 工厂。
	public static void promote(BootstrapContext bootstrapContext, ConfigurableListableBeanFactory beanFactory) {
		TextEncryptor textEncryptor = bootstrapContext.get(TextEncryptor.class);
		if (textEncryptor != null) {
			beanFactory.registerSingleton("textEncryptor", textEncryptor);
		}
	}

	/**
	 * Utility to create a {@link TextEncryptor} via properties.
	 * @param keyProperties the Key properties.
	 * @param rsaProperties RSA properties.
	 * @return created {@link TextEncryptor}.
	 */
	// 通过属性创建 {@link TextEncryptor} 的实用程序。
	// @param keyProperties 密钥属性。
	// @param rsaProperties RSA 属性。
	// @return 创建了 {@link TextEncryptor}。
	public static TextEncryptor createTextEncryptor(KeyProperties keyProperties, RsaProperties rsaProperties) {
		KeyProperties.KeyStore keyStore = keyProperties.getKeyStore();
		if (keyStore.getLocation() != null) {
			if (keyStore.getLocation().exists()) {
				return new RsaSecretEncryptor(
						new KeyStoreKeyFactory(keyStore.getLocation(), keyStore.getPassword().toCharArray(),
								keyStore.getType())
							.getKeyPair(keyStore.getAlias(), keyStore.getSecret().toCharArray()),
						rsaProperties.getAlgorithm(), rsaProperties.getSalt(), rsaProperties.isStrong());
			}

			throw new IllegalStateException("Invalid keystore location");
		}

		return new EncryptorFactory(keyProperties.getSalt()).create(keyProperties.getKey());
	}

	/**
	 * Is a key configured.
	 * @param properties the Key properties.
	 * @return true if configured.
	 */
	// 是否已配置密钥。
	// @param properties 密钥属性。
	// @return 如果已配置，则返回 true。
	public static boolean keysConfigured(KeyProperties properties) {
		if (hasProperty(properties.getKeyStore().getLocation())) {
			if (hasProperty(properties.getKeyStore().getPassword())) {
				return true;
			}
			return false;
		}
		else if (hasProperty(properties.getKey())) {
			return true;
		}
		return false;
	}

	static boolean hasProperty(Object value) {
		if (value instanceof String) {
			return StringUtils.hasText((String) value);
		}
		return value != null;
	}

	/**
	 * Method to check if legacy bootstrap mode is enabled. This is either if the boot
	 * legacy processing property is set or spring.cloud.bootstrap.enabled=true.
	 * @param environment where to check properties.
	 * @return true if bootstrap enabled.
	 */
	// 用于检查旧式引导模式是否已启用的方法。检查方式为：设置引导旧式处理属性，或 spring.cloud.bootstrap.enabled=true。
	// @param environment 用于检查属性的位置。
	// @return true 表示已启用引导。
	public static boolean isLegacyBootstrap(Environment environment) {
		boolean isLegacy = PropertyUtils.useLegacyProcessing(environment);
		boolean isBootstrapEnabled = PropertyUtils.bootstrapEnabled(environment);
		return isLegacy || isBootstrapEnabled;
	}

	/**
	 * TextEncryptor that just fails, so that users don't get a false sense of security
	 * adding ciphers to config files and not getting them decrypted.
	 *
	 * @author Dave Syer
	 *
	 */
	// TextEncryptor 刚刚失败，因此用户不会因为将密码添加到配置文件而无法解密而产生虚假的安全感。
	public static class FailsafeTextEncryptor implements TextEncryptor {

		private TextEncryptor delegate;

		/**
		 * You can set a delegate that can be used to encrypt/decrypt values if later on
		 * after the initial initialization of the app we have the necessary values to
		 * create a proper {@link TextEncryptor}. Depending on where the encryption keys
		 * are set we might not have the right values to create a {@link TextEncryptor}
		 * (this can happen if the keys are in application.properties for example, but we
		 * create the text encryptor during Bootstrap). The delegate functionality allows
		 * us the option to set the delegate later on when we have the necessary values.
		 * @param delegate The TextEncryptor to use for encryption/decryption
		 */
		// 如果在应用初始化完成后，我们拥有创建合适的 {@link TextEncryptor} 所需的值，则可以设置一个委托，用于加密/解密值。
		// 根据加密密钥的设置位置，我们可能没有正确的值来创建 {@link TextEncryptor}
		// （例如，如果密钥位于 application.properties 中，但我们在 Bootstrap 期间创建了文本加密器，则可能会发生这种情况）。
		// 委托功能允许我们在获得必要的值后再设置委托。
		// @param delegate 用于加密/解密的 TextEncryptor
		public void setDelegate(TextEncryptor delegate) {
			this.delegate = delegate;
		}

		public TextEncryptor getDelegate() {
			return this.delegate;
		}

		@Override
		public String encrypt(String text) {
			if (this.delegate != null) {
				return this.delegate.encrypt(text);
			}
			throw new UnsupportedOperationException(
					"No encryption for FailsafeTextEncryptor. Did you configure the keystore correctly?");
		}

		@Override
		public String decrypt(String encryptedText) {
			if (this.delegate != null) {
				return this.delegate.decrypt(encryptedText);
			}
			throw new UnsupportedOperationException(
					"No decryption for FailsafeTextEncryptor. Did you configure the keystore correctly?");
		}

	}

}
