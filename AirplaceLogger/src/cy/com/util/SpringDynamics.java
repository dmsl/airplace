/*
 * AirPlace:  The Airplace Project is an OpenSource Indoor and Outdoor
 * Localization solution using WiFi RSS (Receive Signal Strength).
 * The AirPlace Project consists of three parts:
 *
 *  1) The AirPlace Logger (Ideal for collecting RSS Logs)
 *  2) The AirPlace Server (Ideal for transforming the collected RSS logs
 *  to meaningful RadioMap files)
 *  3) The AirPlace Tracker (Ideal for using the RadioMap files for
 *  indoor localization)
 *
 * It is ideal for spaces where GPS signal is not sufficient.
 *
 * Authors:
 * C. Laoudias, G.Larkou, G. Constantinou, M. Constantinides, S. Nicolaou,
 *
 * Supervisors:
 * D. Zeinalipour-Yazti and C. G. Panayiotou
 *
 * Copyright (c) 2011, KIOS Research Center and Data Management Systems Lab (DMSL),
 * University of Cyprus. All rights reserved.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Υou should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/


/*
 * Copyright (c) 2010, Sony Ericsson Mobile Communication AB. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *
 *    * Redistributions of source code must retain the above copyright notice, this 
 *      list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 *    * Neither the name of the Sony Ericsson Mobile Communication AB nor the names
 *      of its contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package cy.com.util;

/**
 * SpringDynamics is a Dynamics object that uses friction and spring physics to
 * snap to boundaries and give a natural and organic dynamic.
 */
public class SpringDynamics extends Dynamics {

    /** Friction factor */
    private float mFriction;

    /** Spring stiffness factor */
    private float mStiffness;

    /** Spring damping */
    private float mDamping;

    /**
     * Set friction parameter, friction physics are applied when inside of snap
     * bounds.
     * 
     * @param friction Friction factor
     */
    public void setFriction(float friction) {
        mFriction = friction;
    }

    /**
     * Set spring parameters, spring physics are applied when outside of snap
     * bounds.
     * 
     * @param stiffness Spring stiffness
     * @param dampingRatio Damping ratio, < 1 underdamped, > 1 overdamped
     */
    public void setSpring(float stiffness, float dampingRatio) {
        mStiffness = stiffness;
        mDamping = dampingRatio * 2 * (float)Math.sqrt(stiffness);
    }

    /**
     * Calculate acceleration at the current state
     * 
     * @return Current acceleration
     */
    private float calculateAcceleration() {
        float acceleration;

        final float distanceFromLimit = getDistanceToLimit();
        if (distanceFromLimit != 0) {
            acceleration = distanceFromLimit * mStiffness - mDamping * mVelocity;
        } else {
            acceleration = -mFriction * mVelocity;
        }

        return acceleration;
    }

    @Override
    protected void onUpdate(int dt) {
        // Calculate dt in seconds as float
        final float fdt = dt / 1000f;

        // Calculate current acceleration
        final float a = calculateAcceleration();

        // Calculate next position based on current velocity and acceleration
        mPosition += mVelocity * fdt + .5f * a * fdt * fdt;

        // Update velocity
        mVelocity += a * fdt;
    }

}
