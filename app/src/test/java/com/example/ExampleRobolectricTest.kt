package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.repository.MediaRepository
import com.example.util.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CastBeam", appName)
  }

  @Test
  fun `qr code encodes local url properly`() {
    val matrix = QrCodeGenerator.encode("http://192.168.1.15:8080")
    assertNotNull(matrix)
    assertTrue(matrix.isNotEmpty())
    assertTrue(matrix[0].isNotEmpty())
    // Finder pattern check at (0,0)
    assertTrue(matrix[0][0])
  }

  @Test
  fun `media repository loads demo items`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = MediaRepository(context)
    val folders = repo.getSharedFolders()
    assertNotNull(folders)
  }
}
