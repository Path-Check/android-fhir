/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.workflow

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngineProvider
import com.google.common.truth.Truth.assertThat
import java.io.InputStream
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FhirOperatorLibraryEvaluateTest {

  private val fhirEngine =
    FhirEngineProvider.getInstance(ApplicationProvider.getApplicationContext())
  private val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
  private val fhirOperator = FhirOperator(fhirContext, fhirEngine)

  private val json = fhirContext.newJsonParser()

  fun open(assetName: String): InputStream? {
    return javaClass.getResourceAsStream(assetName)
  }

  @Before
  fun setUp() = runBlocking {
    fhirEngine.create(
      json.parseResource(open("/covid-check/COVIDImmunizationHistory.json")) as Resource
    )
    fhirOperator.loadLibs(
      json.parseResource(open("/covid-check/COVIDCheck-FHIRLibraryBundle.json")) as Bundle
    )
  }

  @Test
  fun evaluateCOVIDCheck() = runBlocking {
    assertThat(fhirEngine.get(ResourceType.Patient, "#1")).isNotNull()
    assertThat(fhirEngine.get(ResourceType.Immunization, "#2")).isNotNull()
    assertThat(fhirEngine.get(ResourceType.Organization, "#3")).isNotNull()

    val results =
      fhirOperator.evaluateLibrary(
        "http://localhost/Library/COVIDCheck|1.0.0",
        "#1",
        setOf(
          "CompletedImmunization",
          "GetFinalDose",
          "GetSingleDose",
          "ModernaProtocol",
          "PfizerProtocol"
        )
      ) as
        Parameters

    assertThat(results.getParameterBool("CompletedImmunization")).isTrue()
    assertThat(results.getParameterBool("ModernaProtocol")).isFalse()
    assertThat(results.getParameterBool("PfizerProtocol")).isFalse()
  }
}
