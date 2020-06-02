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

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DateFormatConstraintValidator extends StringConstraintValidator {

    private boolean init = false;

    private SimpleDateFormat dateFormat;

    @Override
    public void initialize(String ... parameters) {
        try {
            if (parameters != null && parameters.length > 0) {
                dateFormat = new SimpleDateFormat(parameters[0]);
                dateFormat.setLenient(false);
                init = true;
            }
        } catch (Throwable t) {
        }
    }

    @Override
    public boolean isValid(String value) {
    	if (value == null || !init) {
    		return false;
    	}

         try {
             dateFormat.parse(value);
             return true;
         } catch (ParseException pex) {
             return false;
         }
    }

    @Override
    public String getMessageTemplate() {
        return "'%s' is not valid (format: '%s')";
    }

}
