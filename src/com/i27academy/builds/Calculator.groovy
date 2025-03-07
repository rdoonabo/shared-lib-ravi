pacakge com.i27academy.builds

class Calculator(jenkins){
   def jenkins
   Calculator(jenkins){
      this.jenkins = jenkins
}
def add(firstNumber, secondNumber) {
    return firstNumber+secondNumber
}
}

