package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.presentation.calendar.HolidayCalendarScreen
import com.example.presentation.viewmodel.PoshanViewModel
import com.example.ui.theme.PoshanTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CG MDM Manager", appName)
  }

  @Test
  fun `render holiday calendar screen without crash`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = PoshanViewModel(application)
    composeTestRule.setContent {
      PoshanTheme {
        HolidayCalendarScreen(
          viewModel = viewModel,
          onNavigateBack = {}
        )
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `render poshan app root without crash`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = PoshanViewModel(application)
    composeTestRule.setContent {
      com.example.presentation.navigation.PoshanAppRoot(viewModel = viewModel)
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `updating school profile headmaster name synchronizes user role account`() = kotlinx.coroutines.runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = PoshanViewModel(application)
    val db = com.example.data.local.AppDatabase.getDatabase(application)
    com.example.data.local.AppDatabase.populateInitialMasterData(db)

    val existingSchool = db.schoolDao().getSchoolDirect() ?: com.example.data.local.entity.SchoolEntity(
      schoolId = "SCH-001",
      udiseCode = "22080100308",
      schoolName = "Govt Middle School",
      stateName = "Chhattisgarh",
      districtName = "Kawardha",
      blockName = "Bodla",
      clusterName = "Bodla",
      villageName = "Bodla",
      schoolType = "Middle",
      headTeacherName = "Shri Old HM",
      headTeacherMobile = "9826012345"
    )

    val updatedSchool = existingSchool.copy(
      headTeacherName = "श्रीमती विमला मरकाम",
      headTeacherMobile = "9876543210"
    )

    viewModel.saveSchool(updatedSchool)
    // Allow coroutines to settle
    kotlinx.coroutines.delay(200)

    val headmasterUser = db.userDao().getHeadmasterDirect()
    org.junit.Assert.assertNotNull("Headmaster user should exist", headmasterUser)
    assertEquals("श्रीमती विमला मरकाम", headmasterUser?.name)
    assertEquals("9876543210", headmasterUser?.mobile)
  }

  @Test
  fun `updating headmaster user role synchronizes school profile`() = kotlinx.coroutines.runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = PoshanViewModel(application)
    val db = com.example.data.local.AppDatabase.getDatabase(application)
    com.example.data.local.AppDatabase.populateInitialMasterData(db)

    val currentHm = db.userDao().getHeadmasterDirect()
    org.junit.Assert.assertNotNull(currentHm)

    val updatedHm = currentHm!!.copy(
      name = "श्री श्यामलाल वर्मा",
      mobile = "9988776655"
    )

    viewModel.saveUserStaff(updatedHm)
    kotlinx.coroutines.delay(200)

    val school = db.schoolDao().getSchoolDirect()
    org.junit.Assert.assertNotNull(school)
    assertEquals("श्री श्यामलाल वर्मा", school?.headTeacherName)
    assertEquals("9988776655", school?.headTeacherMobile)
  }

  @Test
  fun `google drive backup snapshot serialization and restore test`() = kotlinx.coroutines.runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val db = com.example.data.local.AppDatabase.getDatabase(application)
    com.example.data.local.AppDatabase.populateInitialMasterData(db)

    val engine = com.example.cloud.GoogleDriveBackupEngine(application, db)
    val payload = engine.createSnapshotPayload("Test HM")

    org.junit.Assert.assertNotNull(payload)
    org.junit.Assert.assertTrue(payload.metadata.backupId.startsWith("CG_MDM_") || payload.metadata.backupId.startsWith("POSHAN_"))
    assertEquals("22080100308", payload.metadata.udiseCode)

    // Serialization test
    val json = com.example.cloud.GoogleDriveBackupSerializer.serialize(payload)
    org.junit.Assert.assertTrue(json.isNotBlank())
    org.junit.Assert.assertTrue(json.contains("CG_MDM_") || json.contains("POSHAN_"))

    // Deserialization test
    val restored = com.example.cloud.GoogleDriveBackupSerializer.deserialize(json)
    assertEquals(payload.metadata.backupId, restored.metadata.backupId)
    assertEquals(payload.metadata.udiseCode, restored.metadata.udiseCode)
    assertEquals(payload.school?.schoolName, restored.school?.schoolName)
  }

  @Test
  fun `render google drive backup screen without crash`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = PoshanViewModel(application)
    composeTestRule.setContent {
      PoshanTheme {
        com.example.presentation.backup.GoogleDriveBackupScreen(
          poshanViewModel = viewModel,
          onNavigateBack = {}
        )
      }
    }
    composeTestRule.waitForIdle()
  }
}
