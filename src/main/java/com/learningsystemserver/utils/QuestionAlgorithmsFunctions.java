package com.learningsystemserver.utils;

public class QuestionAlgorithmsFunctions {

    //.......................................addition--------------------------------------------------------------------

    public static String simplifyAddition(int a, int b, int answer) {
        boolean aIsNegative = a < 0;
        boolean bIsNegative = b < 0;

        int posA = Math.abs(a);
        int posB = Math.abs(b);
        if (posA < 10 && posB < 10 && answer <= 10) {
            // Fixed typo: "then" -> "them"
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

        // Standardized "from the X we only have" to lowercase for easier translation matching
        if (a >= 100 || b >= 100) {
            step2 = "Step 2: Combine the parts:\n";
            if(aOnes != 0 && bOnes != 0) { step2 += "Combine the ones: " + aOnes + " + " + bOnes + " = " + (aOnes + bOnes) + "\n";}else if(((aOnes + bOnes)==0)){}else {step2 += "From the ones we only have: "+Math.max(bOnes,aOnes)+"\n";}
            if(aTens != 0 && bTens != 0) {step2 += "Combine the tens: " + aTens + " + " + bTens + " = " + (aTens + bTens) + "\n";}else if((aTens + bTens)==0){step2+="";}else {step2 += "From the tens we only have: "+Math.max(bTens,aTens)+"\n";}
            if(aHundreds != 0 && bHundreds != 0) { step2 += "Combine the hundreds: " + aHundreds + " + " + bHundreds + " = " + (aHundreds + bHundreds) + "\n";}else if (((aHundreds + bHundreds)==0)){step2+="";}else {step2 += "From the hundreds we only have: "+Math.max(bHundreds,aHundreds)+"\n";}
            int totalHundreds = aHundreds + bHundreds;
            String step3 = "Step 3: Now combine all the parts: " + totalHundreds + " + " + (aTens + bTens) + " + " + (aOnes + bOnes) + " = " + answer;
            return step1 + "\n\n" + step2 + "\n" + step3;
        } else {
            step2 = "Step 2: Combine the parts:\n";
            if (aTens != 0 || bTens != 0) { step2 += "Combine the tens: " + aTens + " + " + bTens + " = " + (aTens + bTens) + "\n";}else if((aTens + bTens)==0){}else {step2 += "From the ones we only have: "+Math.max(bOnes,aOnes)+"\n";}
            if (aOnes != 0 || bOnes != 0) {step2 += "Combine the ones: " + aOnes + " + " + bOnes + " = " + (aOnes + bOnes) + "\n";}else if((aOnes + bOnes)==0){step2+="";}else {step2 += "From the tens we only have: "+Math.max(bTens,aOnes)+"\n";}

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


    //.......................................subtraction Simplification................................................................
    public static String simplifySubtraction(int a, int b, int answer) {
        boolean aIsNegative = a < 0;
        boolean bIsNegative = b < 0;

        int posA = Math.abs(a);
        int posB = Math.abs(b);
        if (posA < 10 && posB < 10 && answer <= 10) {
            return "To subtract " + b + " from " + a + ", simply subtract and then you have " + answer + ".";
        } else if (posA < 10 && posB < 10) {
            int bigger = Math.max(a, b);
            int smaller = Math.min(a, b);
            int amountToTake = 10 - bigger;
            int remaining = smaller - amountToTake;
            // Fixed typo: "subtract a in b" -> "subtract b from a"
            return "To subtract " + b + " from " + a + ", take " + amountToTake + " from " + smaller + " and subtract it from " + bigger + " to make 10, then add the remaining " + remaining + " to get " + answer + ".";
        } else if (posA % 10 == 0 && posB < 10) {
            return "To subtract " + b + " from " + a + ", simply subtract " + b + " from " + a + " to get " + answer + ".";
        } else if (posB % 10 == 0 && posA < 10) {
            // Fixed typo: "subtract a in b" -> "subtract a from b"
            return "To subtract " + a + " from " + b + ", simply subtract " + a + " from " + b + " to get " + answer + ".";
        } else {
            return simplifyMultiDigitSub(a, b, answer, aIsNegative, bIsNegative);
        }
    }

    private static String simplifyMultiDigitSub(int a, int b, int answer, boolean aIsNegative, boolean bIsNegative) {
        String aParts = getNumberPartsSub(a, aIsNegative);
        String bParts = getNumberPartsSub(b, bIsNegative);

        if ((Math.abs(a) < 10 && Math.abs(b) >= 10 && Math.abs(b) < 100) || (Math.abs(b) < 10 && Math.abs(a) >= 10 && Math.abs(a) < 100)) {
            if (Math.abs(a) < 10 && Math.abs(b) >= 10) {
                if (Math.abs(b) + Math.abs(a) < 100) {
                    // Fixed phrasing
                    return "To subtract " + b + " from " + a + ", simply subtract " + b + " from " + a + " to get " + answer + ".";
                }
            }
            if (Math.abs(b) < 10 && Math.abs(a) >= 10) {
                if (Math.abs(a) + Math.abs(b) < 100) {
                    // Fixed phrasing
                    return "To subtract " + b + " from " + a + ", simply subtract " + b + " from " + a + " to get " + answer + ".";
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

        // Standardized "from the X we only have"
        if (a >= 100 || b >= 100) {
            step2 = "Step 2: subtract the parts:\n";
            if(aOnes != 0 && bOnes != 0) { step2 += "subtract the ones: " + aOnes + " - " + bOnes + " = " + (aOnes - bOnes) + "\n";}else if(((aOnes - bOnes)==0)){}else {step2 += "From the ones we only have: "+Math.max(bOnes,aOnes)+"\n";}
            if(aTens != 0 && bTens != 0) {step2 += "subtract the tens: " + aTens + " - " + bTens + " = " + (aTens - bTens) + "\n";}else if((aTens - bTens)==0){step2+="";}else {step2 += "From the tens we only have: "+Math.max(bTens,aTens)+"\n";}
            if(aHundreds != 0 && bHundreds != 0) { step2 += "subtract the hundreds: " + aHundreds + " - " + bHundreds + " = " + (aHundreds - bHundreds) + "\n";}else if (((aHundreds - bHundreds)==0)){step2+="";}else {step2 += "From the hundreds we only have: "+Math.max(bHundreds,aHundreds)+"\n";}
            int totalHundreds = aHundreds + bHundreds;
            String step3 = "Step 3: Now subtract all the parts: " + totalHundreds + " - " + (aTens - bTens) + " - " + (aOnes - bOnes) + " = " + answer;
            return step1 + "\n\n" + step2 + "\n" + step3;
        } else {
            step2 = "Step 2: subtract the parts:\n";
            if (aTens != 0 || bTens != 0) { step2 += "subtract the tens: " + aTens + " - " + bTens + " = " + (aTens - bTens) + "\n";}else if((aTens - bTens)==0){}else {step2 += "From the ones we only have: "+Math.max(bOnes,aOnes)+"\n";}
            if (aOnes != 0 || bOnes != 0) {step2 += "subtract the ones: " + aOnes + " - " + bOnes + " = " + (aOnes - bOnes) + "\n";}else if((aOnes - bOnes)==0){step2+="";}else {step2 += "From the tens we only have: "+Math.max(bTens,aOnes)+"\n";}

            int totalOnes = aOnes - bOnes;
            int totalTens = aTens - bTens;
            String step3 = "Step 3: Now subtract all the parts: " + totalTens + " - " + totalOnes + " = " + answer;
            return step1 + "\n\n" + step2 + "\n" + step3;
        }
    }

    private static String getNumberPartsSub(int num, boolean isNegative) {
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
                result += " and ";
            }
            result += tensString;
        }
        if (!onesString.isEmpty()) {
            if (!hundredsString.isEmpty() ||!tensString.isEmpty()) {
                result += " and ";
            }
            result += onesString;
        }

        return result;
    }
    //.......................................Fraction Simplification................................................................

    public static String simplifyFractions(int num1, int den1, int num2, int den2, int sumNum, int commonDen) {
        int gcd = findGCD(sumNum, commonDen);
        int simplifiedNum = sumNum / gcd;
        int simplifiedDen = commonDen / gcd;
        return "1) Common denominator: " + den1 + " * " + den2 + " = " + commonDen
                + "\n2) Convert each fraction: " + num1 + "/" + den1
                + " = " + (num1 * den2) + "/" + commonDen + " and " + num2 + "/" + den2
                + " = " + (num2 * den1) + "/" + commonDen
                + "\n3) Add numerators: " + (num1 * den2) + " + " + (num2 * den1) + " = " + sumNum
                + "\n4) Final fraction: " + sumNum + "/" + commonDen
                + " which simplifies to " + simplifiedNum + "/" + simplifiedDen + ".";
    }

    //.......................................Division Simplification................................................................

    public static String simplifyDivision(int a, int b, int answer) {
        boolean aIsNegative = a < 0;
        boolean bIsNegative = b < 0;

        if (b == 0) {
            return "Division by zero is undefined.";
        }

        String sign = (aIsNegative ^ bIsNegative) ? "-" : "";
        String step1 = "Step 1: Divide the numbers: " + Math.abs(a) + " / " + Math.abs(b) + " = " + answer;
        String step2 = "Step 2: Apply the sign: " + sign + answer;

        return step1 + "\n" + step2;
    }

    //.......................................Multiplication Simplification................................................................

    public static String simplifyMultiplication(int a, int b, int answer) {
        int result = 0;
        int multiplier = 1;
        int tempB = b;
        int i = 1;
        String output = "";
        while(tempB > 0){
            int currentDigit = tempB % 10;
            tempB /= 10;
            output += i+") Multiply " + a + " by " + currentDigit + " (from " + b + ")";
            int partialProduct = a * currentDigit;
            output+="\n"+a+" * " +currentDigit+ " = "+partialProduct;
            output+="\n"+partialProduct+" * " +multiplier + " = " + partialProduct * multiplier;
            partialProduct *= multiplier;
            output+="\n"+result+" + " +partialProduct;
            result += partialProduct;
            output+="\nCurrent result: "+result+"\n";

            multiplier*=10;
            i++;
            output+="\n";
        }

        return output;
    }

    //.......................................Rectangle Simplification................................................................

    public static String simplifyRectangle(int length, int width, int area, int perimeter) {
        return "1) Area = length * width = " + length + " * " + width + " = " + area
                + "\n2) Perimeter = 2 * (length + width) = 2 * (" + length + " + " + width + ") = " + perimeter + ".";
    }

    //.......................................Circle Simplification................................................................

    public static String simplifyCircle(int radius, double area, double circumference) {
        return "1) Area = pi * r^2 = 3.14 * " + radius + "^2 = " + String.format("%.2f", area)
                + "\n2) Circumference = 2 * pi * r = 2 * 3.14 * " + radius + " = " + String.format("%.2f", circumference) + ".";
    }

    //.......................................Triangle Simplification................................................................

    public static String simplifyTriangle(int base, int height, double area, double hypotenuse) {
        return "1) Area = 1/2 * base * height = 1/2 * " + base + " * " + height + " = " + String.format("%.2f", area)
                + "\n2) Hypotenuse = sqrt(base^2 + height^2) = sqrt(" + base + "^2 + " + height + "^2) = " + String.format("%.2f", hypotenuse) + ".";
    }

    //.......................................Polygon Simplification................................................................

    public static String simplifyPolygon(int side, double apothem, double area) {
        return "1) Apothem = side / (2 * tan(pi/5)) = " + side + " / (2 * tan(pi/5)) = " + String.format("%.2f", apothem)
                + "\n2) Area = (5 * side * apothem) / 2 = (5 * " + side + " * " + String.format("%.2f", apothem) + ") / 2 = " + String.format("%.2f", area) + ".";
    }

    //.......................................Helper Methods................................................................

    private static int findGCD(int a, int b) {
        if (b == 0) return a;
        return findGCD(b, a % b);
    }
}