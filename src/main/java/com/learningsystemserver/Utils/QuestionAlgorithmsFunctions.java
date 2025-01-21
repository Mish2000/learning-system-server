package com.learningsystemserver.Utils;

public class QuestionAlgorithmsFunctions {

    //.......................................addition--------------------------------------------------------------------

    public static String simplify(int a, int b, int answer) {
        boolean aIsNegative = a < 0;
        boolean bIsNegative = b < 0;

        int posA = Math.abs(a);
        int posB = Math.abs(b);

        if (posA < 10 && posB < 10 && answer < 10) {
            return "To add " + a + " and " + b + ", simply add them together to get " + answer + ".";
        } else if (posA < 10 && posB < 10) {
            int bigger = Math.max(a, b);
            int smaller = Math.min(a, b);
            int amountToTake = 10 - bigger;
            int remaining = smaller - amountToTake;

            return "To add " + a + " and " + b + ", take " + amountToTake + " from " + smaller + " and add it to " + bigger + " to make 10, then add the remaining " + remaining + " to get " + answer + ".";
        } else if (posA % 10 == 0 && posB < 10) {
            return "To add " + a + " and " + b + ", simply add " + b + " to " + a + " to get " + answer + ".";
        } else if (posB % 10 == 0 && posA < 10) {
            return "To add " + a + " and " + b + ", simply add " + a + " to " + b + " to get " + answer + ".";
        } else {
            return simplifyMultiDigit(a, b, answer, aIsNegative, bIsNegative);
        }
    }

    private static String simplifyMultiDigit(int a, int b, int answer, boolean aIsNegative, boolean bIsNegative) {
        String aParts = getNumberParts(a, aIsNegative);
        String bParts = getNumberParts(b, bIsNegative);

        if ((Math.abs(a) < 10 && Math.abs(b) >= 10 && Math.abs(b) < 100) || (Math.abs(b) < 10 && Math.abs(a) >= 10 && Math.abs(a) < 100)) {
            if (Math.abs(a) < 10 && Math.abs(b) >= 10) {
                if (Math.abs(b) + Math.abs(a) < 100) {
                    return "To add " + a + " and " + b + ", simply add " + a + " to " + b + " to get " + answer + ".";
                }
            }
            if (Math.abs(b) < 10 && Math.abs(a) >= 10) {
                if (Math.abs(a) + Math.abs(b) < 100) {
                    return "To add " + a + " and " + b + ", simply add " + b + " to " + a + " to get " + answer + ".";
                }
            }
        }

        String step1 = "Step 1: Write the numbers:\n" + aParts + "\n" + bParts;

        String step2;
        int aHundreds = (Math.abs(a) / 100) * 100;
        int aTens = ((Math.abs(a) % 100) / 10) * 10;
        int aOnes = Math.abs(a) % 10;
        int bHundreds = (Math.abs(b) / 100) * 100;
        int bTens = ((Math.abs(b) % 100) / 10) * 10;
        int bOnes = Math.abs(b) % 10;

        if (a >= 100 || b >= 100) {
            step2 = "Step 2: Combine the parts:\n";
            step2 += "Combine the ones: " + aOnes + " + " + bOnes + " = " + (aOnes + bOnes) + "\n";
            step2 += "Combine the tens: " + aTens + " + " + bTens + " = " + (aTens + bTens) + "\n";
            step2 += "Combine the hundreds: " + aHundreds + " + " + bHundreds + " = " + (aHundreds + bHundreds) + "\n";
            int totalOnes = aOnes + bOnes;
            int totalTens = aTens + bTens;
            int totalHundreds = aHundreds + bHundreds;
            String step3 = "Step 3: Now combine all the parts: " + totalHundreds + " + " + (aTens + bTens) + " + " + (aOnes + bOnes) + " = " + answer;
            return step1 + "\n" + step2 + "\n" + step3;
        } else {
            step2 = "Step 2: Combine the parts:\n";
            step2 += "Combine the tens: " + aTens + " + " + bTens + " = " + (aTens + bTens) + "\n";
            step2 += "Combine the ones: " + aOnes + " + " + bOnes + " = " + (aOnes + bOnes) + "\n";
            int totalOnes = aOnes + bOnes;
            int totalTens = aTens + bTens;
            String step3 = "Step 3: Now combine all the parts: " + totalTens + " + " + totalOnes + " = " + answer;
            return step1 + "\n\n" + step2 + "\n" + step3;
        }
    }

    private static String getNumberParts(int num, boolean isNegative) {
        int hundreds = (Math.abs(num) / 100) * 100;
        int tens = ((Math.abs(num) % 100) / 10) * 10;
        int ones = Math.abs(num) % 10;

        String sign = isNegative? "-" : "";
        String hundredsString = hundreds > 0? hundreds + " (hundreds)" : "";
        String tensString = tens > 0? tens + " (tens)" : "";
        String onesString = ones > 0? ones + " (ones)" : "";

        String result = sign + Math.abs(num) + " = ";
        if (!hundredsString.isEmpty()) {
            result += hundredsString;
        }
        if (!tensString.isEmpty()) {
            if (!hundredsString.isEmpty()) {
                result += " + ";
            }
            result += tensString;
        }
        if (!onesString.isEmpty()) {
            if (!hundredsString.isEmpty() ||!tensString.isEmpty()) {
                result += " + ";
            }
            result += onesString;
        }

        return result;
    }

    //.......................................reduction--------------------------------------------------------------------



    //.......................................divide--------------------------------------------------------------------



    //.......................................multiply--------------------------------------------------------------------



}
