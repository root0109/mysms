/**
 * The payment gateway MySMS takes money through (Razorpay), shared by the modules that sell something: finance (school
 * fees) and platform (subscriptions). Each module keeps its own gateway account and its own records; this package only
 * speaks the gateway's protocol and checks its signatures.
 */
package com.mysms.core.payment;
