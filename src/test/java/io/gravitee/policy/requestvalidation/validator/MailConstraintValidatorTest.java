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
public class MailConstraintValidatorTest {

    @Test
    public void shouldNotValidate_nullValue() {
        MailConstraintValidator validator = new MailConstraintValidator();

        boolean valid = validator.isValid(null);
        Assert.assertFalse(valid);
    }

    @Test
    public void shouldNotValidate_emptyValue() {
        MailConstraintValidator validator = new MailConstraintValidator();

        boolean valid = validator.isValid("");
        Assert.assertFalse(valid);
    }

    @Test
    public void shouldNotValidate_badEmail() {
        MailConstraintValidator validator = new MailConstraintValidator();

        boolean valid = validator.isValid("contact@graviteesource");
        Assert.assertFalse(valid);
    }

    @Test
    public void shouldValidate() {
        MailConstraintValidator validator = new MailConstraintValidator();

        boolean valid = validator.isValid("contact@graviteesource.com");
        Assert.assertTrue(valid);
    }

    @Test
    public void shouldValidateEncoded() {
        MailConstraintValidator validator = new MailConstraintValidator();

        boolean valid = validator.isValid("contact%40graviteesource.com");
        Assert.assertTrue(valid);
    }
}
