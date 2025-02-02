/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.commons.logging.logback

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.sift.SiftingAppender
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Context
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy
import ch.qos.logback.core.sift.AppenderFactory
import ch.qos.logback.core.status.Status
import ch.qos.logback.core.status.StatusManager
import ch.qos.logback.core.util.Duration
import ch.qos.logback.core.util.FileSize
import io.airbyte.commons.logging.DEFAULT_LOG_FILENAME
import io.airbyte.commons.storage.DocumentType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.pathString

private class AirbyteLogbackCustomConfigurerTest {
  private lateinit var configurer: AirbyteLogbackCustomConfigurer

  @BeforeEach
  fun setUp() {
    configurer = AirbyteLogbackCustomConfigurer()
  }

  @Test
  fun testCreateApplicationRollingAppender() {
    val context =
      mockk<Context> {
        every { getObject(any()) } returns mutableMapOf<String, String>()
        every { statusManager } returns
          mockk<StatusManager> {
            every { add(any<Status>()) } returns Unit
          }
      }
    val discriminatorValue = Files.createTempDirectory("test-1").pathString
    val appender = configurer.createApplicationRollingAppender(loggerContext = context, discriminatorValue = discriminatorValue)

    assertEquals(RollingFileAppender::class.java, appender.javaClass)
    assertEquals(context, appender.context)
    assertEquals("$discriminatorValue-local", appender.name)
    assertEquals(
      AirbytePlatformLogbackMessageLayout::class.java,
      ((appender as OutputStreamAppender).encoder as LayoutWrappingEncoder).layout.javaClass,
    )
    assertEquals("$discriminatorValue/$DEFAULT_LOG_FILENAME", (appender as RollingFileAppender).file)

    assertEquals(FixedWindowRollingPolicy::class.java, appender.rollingPolicy.javaClass)
    assertEquals(
      "$discriminatorValue/$DEFAULT_LOG_FILENAME".replace(LOG_FILE_EXTENSION, ROLLING_FILE_NAME_PATTERN),
      (appender.rollingPolicy as FixedWindowRollingPolicy).fileNamePattern,
    )
    assertEquals(3, (appender.rollingPolicy as FixedWindowRollingPolicy).maxIndex)

    assertEquals(SizeBasedTriggeringPolicy::class.java, appender.triggeringPolicy.javaClass)
    assertEquals(FileSize.valueOf(DEFAULT_MAX_LOG_FILE_SIZE), (appender.triggeringPolicy as SizeBasedTriggeringPolicy).maxFileSize)

    assertTrue(appender.isStarted)
    assertTrue(Path.of(appender.file).exists())
  }

  @Test
  fun testCreateOperationsJobFileAppender() {
    val context =
      mockk<Context> {
        every { getObject(any()) } returns mutableMapOf<String, String>()
        every { statusManager } returns
          mockk<StatusManager> {
            every { add(any<Status>()) } returns Unit
          }
      }
    val discriminatorValue = Files.createTempDirectory("test-2").pathString
    val appender = configurer.createOperationsJobFileAppender(loggerContext = context, discriminatorValue = discriminatorValue)

    assertEquals(FileAppender::class.java, appender.javaClass)
    assertEquals(context, appender.context)
    assertEquals("$discriminatorValue/$DEFAULT_LOG_FILENAME", (appender as FileAppender).file)
    assertEquals("$discriminatorValue-local", appender.name)
    assertEquals(
      AirbyteOperationsJobLogbackMessageLayout::class.java,
      ((appender as OutputStreamAppender).encoder as LayoutWrappingEncoder).layout.javaClass,
    )

    assertTrue(appender.isStarted)
    assertTrue(Path.of(appender.file).exists())
  }

  @Test
  fun testCreateOperationsJobFileAppenderWithFileDiscriminator() {
    val context =
      mockk<Context> {
        every { getObject(any()) } returns mutableMapOf<String, String>()
        every { statusManager } returns
          mockk<StatusManager> {
            every { add(any<Status>()) } returns Unit
          }
      }
    val discriminatorValue = Files.createTempFile("test-2", "other.log").pathString
    val appender = configurer.createOperationsJobFileAppender(loggerContext = context, discriminatorValue = discriminatorValue)

    assertEquals(FileAppender::class.java, appender.javaClass)
    assertEquals(context, appender.context)
    assertEquals(discriminatorValue, (appender as FileAppender).file)
    assertEquals("$discriminatorValue-local", appender.name)
    assertEquals(
      AirbyteOperationsJobLogbackMessageLayout::class.java,
      ((appender as OutputStreamAppender).encoder as LayoutWrappingEncoder).layout.javaClass,
    )

    assertTrue(appender.isStarted)
    assertTrue(Path.of(appender.file).exists())
  }

  @Test
  fun testCreatePlatformConsoleAppender() {
    val context =
      mockk<LoggerContext> {
        every { getObject(any()) } returns mutableMapOf<String, String>()
        every { statusManager } returns
          mockk<StatusManager> {
            every { add(any<Status>()) } returns Unit
          }
      }
    val appender = configurer.createPlatformAppender(loggerContext = context)
    assertEquals(context, appender.context)
    assertEquals(PLATFORM_LOGGER_NAME, appender.name)
    assertEquals(
      AirbytePlatformLogbackMessageLayout::class.java,
      ((appender as OutputStreamAppender<ILoggingEvent>).encoder as LayoutWrappingEncoder).layout.javaClass,
    )

    assertTrue(appender.isStarted)
  }

  @Test
  fun testCreateAirbyteCloudStorageAppender() {
    val context =
      mockk<Context> {
        every { getObject(any()) } returns mutableMapOf<String, String>()
        every { statusManager } returns
          mockk<StatusManager> {
            every { add(any<Status>()) } returns Unit
          }
      }
    val appenderName = "test-appender"
    val discriminatorValue = "/workspace/1"
    val documentType = DocumentType.LOGS
    val layout = AirbytePlatformLogbackMessageLayout()
    val appender =
      configurer.createCloudAppender(
        context = context,
        discriminatorValue = discriminatorValue,
        documentType = documentType,
        appenderName = appenderName,
        layout = layout,
      )

    assertEquals(AirbyteCloudStorageAppender::class.java, appender.javaClass)
    assertEquals(context, appender.context)
    assertEquals("$appenderName-$discriminatorValue", appender.name)
    assertEquals(documentType, appender.documentType)
    assertEquals(layout.javaClass, (appender.encoder as LayoutWrappingEncoder).layout.javaClass)
    assertEquals(discriminatorValue, appender.baseStorageId)

    assertTrue(appender.isStarted)
  }

  @Test
  fun testCreateSiftingAppender() {
    val loggerContext =
      mockk<LoggerContext> {
        every { getObject(any()) } returns mutableMapOf<String, String>()
        every { statusManager } returns
          mockk<StatusManager> {
            every { add(any<Status>()) } returns Unit
          }
      }
    val appenderFactory = mockk<AppenderFactory<ILoggingEvent>>()
    val appenderName = "test-appender"
    val contextKey = "test-context-key"
    val appender =
      configurer.createSiftingAppender(
        appenderFactory = appenderFactory,
        contextKey = contextKey,
        appenderName = appenderName,
        loggerContext = loggerContext,
      )

    assertEquals(SiftingAppender::class.java, appender.javaClass)
    assertEquals(loggerContext, appender.context)
    assertEquals(appenderName, appender.name)
    assertEquals(Duration.valueOf("$APPENDER_TIMEOUT minutes").milliseconds, appender.timeout.milliseconds)

    assertTrue(appender.isStarted)
  }
}
