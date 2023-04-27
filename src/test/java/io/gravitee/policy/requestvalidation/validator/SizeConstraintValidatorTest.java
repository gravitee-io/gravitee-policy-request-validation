/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.requestvalidation.validator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SizeConstraintValidatorTest {

    @Test
    void shouldNotValidate_missingParameter() {
        SizeConstraintValidator validator = new SizeConstraintValidator();
        validator.initialize("123");

        boolean valid = validator.isValid("lorem ipsum");
        assertThat(valid).isFalse();
    }

    @Test
    void shouldNotValidate_invalidMaxValue() {
        SizeConstraintValidator validator = new SizeConstraintValidator();
        validator.initialize("12", "123.invalid");

        boolean valid = validator.isValid("lorem ipsum");
        assertThat(valid).isFalse();
    }

    @Test
    void shouldValidate() {
        SizeConstraintValidator validator = new SizeConstraintValidator();
        validator.initialize("1", "23");

        boolean valid = validator.isValid("lorem ipsum");
        assertThat(valid).isTrue();
    }

    @Test
    void shouldNotValidate() {
        SizeConstraintValidator validator = new SizeConstraintValidator();
        validator.initialize("1", "5");

        boolean valid = validator.isValid("lorem ipsum");
        assertThat(valid).isFalse();
    }
}
