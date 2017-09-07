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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MinConstraintValidatorTest {

    @Test
    public void shouldNotValidate_invalidValue() {
        MinConstraintValidator validator = new MinConstraintValidator();
        validator.initialize("123");

        boolean valid = validator.isValid("12.d34");
        Assert.assertFalse(valid);
    }

    @Test
    public void shouldNotValidate_invalidMaxValue() {
        MinConstraintValidator validator = new MinConstraintValidator();
        validator.initialize("123.invalid");

        boolean valid = validator.isValid("12.34");
        Assert.assertFalse(valid);
    }

    @Test
    public void shouldNotValidate() {
        MinConstraintValidator validator = new MinConstraintValidator();
        validator.initialize("123");

        boolean valid = validator.isValid("12.34");
        Assert.assertFalse(valid);
    }

    @Test
    public void shouldValidate() {
        MinConstraintValidator validator = new MinConstraintValidator();
        validator.initialize("123");

        boolean valid = validator.isValid("1243");
        Assert.assertTrue(valid);
    }
}
