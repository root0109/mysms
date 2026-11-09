import axe from 'axe-core';

/**
 * Runs axe on a rendered container; returns the violations (ui/04 §6: zero criticals is the gate).
 * Colour contrast is verified by the token build, and component fragments are not full pages,
 * so the page-level rules are off here.
 */
export async function axeViolations(container: Element) {
  const result = await axe.run(container, {
    rules: {
      'color-contrast': { enabled: false },
      region: { enabled: false },
      'landmark-one-main': { enabled: false },
      'page-has-heading-one': { enabled: false },
    },
  });
  return result.violations.map((v) => `${v.id}: ${v.help}`);
}
