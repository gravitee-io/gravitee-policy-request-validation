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
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PatternConstraintValidatorTest {

	String pattern = "^test$";
	
    @Test
    public void shouldNotValidate_nullValue() {
        PatternConstraintValidator validator = new PatternConstraintValidator();
        validator.initialize(pattern);
        boolean valid = validator.isValid(null);
        Assert.assertFalse(valid);
    }

    @Test
    public void shouldNotValidate_doesNotRespectPattern() {
    	PatternConstraintValidator validator = new PatternConstraintValidator();
    	validator.initialize(pattern);
        boolean valid = validator.isValid("");
        Assert.assertFalse(valid);
    }

    @Test
    public void shouldValidate() {
    	PatternConstraintValidator validator = new PatternConstraintValidator();
    	validator.initialize(pattern);
        boolean valid = validator.isValid("test");
        Assert.assertTrue(valid);
    }

}
