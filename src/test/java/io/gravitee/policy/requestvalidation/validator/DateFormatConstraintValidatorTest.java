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
public class DateFormatConstraintValidatorTest {

    @Test
    public void shouldNotValidate_nullValue() {
        DateFormatConstraintValidator validator = new DateFormatConstraintValidator();
        validator.initialize("dd/MM/yyyy");
        boolean valid = validator.isValid(null);
        Assert.assertFalse(valid);
    }

    @Test
    public void shouldNotValidate_doesNotRespectPattern() {
        DateFormatConstraintValidator validator = new DateFormatConstraintValidator();
        validator.initialize("dd/MM/yyyy");
        boolean valid = validator.isValid("31/20/19991");
        Assert.assertFalse(valid);
    }

    @Test
    public void shouldValidate_simpleFormat() {
        DateFormatConstraintValidator validator = new DateFormatConstraintValidator();
        validator.initialize("dd/MM/yyyy");
        boolean valid = validator.isValid("29/02/2012");
        Assert.assertTrue(valid);
    }

    @Test
    public void shouldValidate_complexFormat() {
        DateFormatConstraintValidator validator = new DateFormatConstraintValidator();
        validator.initialize("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        boolean valid = validator.isValid("2001-07-04T12:08:56.235-0700");
        Assert.assertTrue(valid);
    }
}
