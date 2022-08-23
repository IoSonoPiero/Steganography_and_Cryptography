package cryptography

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.experimental.xor

fun main() {
    printMenu()
}

fun printMenu() {
    while (true) {
        println("Task (hide, show, exit):")
        when (val inputCommand = readln()) {
            "exit" -> {
                println("Bye!")
                return
            }
            "hide" -> hideInImage()
            "show" -> showFromImage()
            else -> println("Wrong task: $inputCommand")
        }
    }
}

fun getInitialInput(): Array<String> {
    println("Input image file:")
    // read the name for input file
    val inputString = readln()

    // read the name for output file
    println("Output image file:")
    val outputString = readln()

    // read the message to hide
    println("Message to hide:")
    val message = readln()

    // read the password to scramble the message
    println("Password:")
    val password = readln()

    return arrayOf(inputString, outputString, message, password)
}

fun hideInImage() {
    val (inputString, outputString, message, password) = getInitialInput()

    // Create a file instance in order to read the inputted image file
    val inputFile = File(inputString)

    val myImage: BufferedImage
    try {
        // Create a BufferedImage instance from the source image file data
        myImage = ImageIO.read(inputFile)
    } catch (e: Exception) {
        println("Can't read input file!")
        return
    }

    // check if input image is not large enough to hold the secret message
    if (!checkImageSize(myImage, message)) {
        println("The input image is not large enough to hold this message.")
        return
    }

    // process the Image and encode the text
    encodeImage(myImage, encodeMessageToArray(message, password))

    println("Input Image: $inputString")
    println("Output Image: $outputString")

    // save the new image
    val stateSave = saveImage(myImage, outputString)
    if (stateSave) {
        println("Message saved in $outputString image.")
    } else {
        println("Image $outputString is not saved.")
    }
}

fun showFromImage() {
    println("Input image file:")
    // read the name for input file
    val inputString = readln()

    println("Password:")
    val password = readln()

    // Create a file instance in order to read the inputted image file
    val inputFile = File(inputString)

    val myImage: BufferedImage
    try {
        // Create a BufferedImage instance from the source image file data
        myImage = ImageIO.read(inputFile)
    } catch (e: Exception) {
        println("Can't read input file!")
        return
    }

    val plainText = decodeMessageFromArray(decodeImage(myImage), password)
    println("Message: $plainText")
}

fun encodeImage(myImage: BufferedImage, encodedMessage: String) {

    // get the iterations to do for the message to encode
    val maxIterations = encodedMessage.length
    var iteration = 0

    // myImage.width is the image width
    // myImage.height is the image height
    for (y in 0 until myImage.height) {          // For every row
        for (x in 0 until myImage.width) {       // For every column.

            // iterate until every bit has been written into BLUE
            if (iteration < maxIterations) {

                // Read color from the (x, y) position
                val color = Color(myImage.getRGB(x, y))

                val red = color.red     // get red color
                val green = color.green // get green color
                val blue = color.blue.and(254).or(encodedMessage[iteration].digitToInt()) % 256

                // Create a new Color instance with LSB
                val colorNew = Color(red, green, blue)
                // Set colorNew at the (x, y) position
                myImage.setRGB(x, y, colorNew.rgb)
                iteration++
            } else {
                return
            }

        }
    }
}

fun decodeImage(myImage: BufferedImage): String {
    // create a list of int to contain the 0s and 1s of the decoded message
    val mInt: MutableList<Int> = mutableListOf()

    // myImage.width is the image width
    // myImage.height is the image height
    for (y in 0 until myImage.height) {          // For every row
        for (x in 0 until myImage.width) {       // For every column.

            // Read color from the (x, y) position
            val color = Color(myImage.getRGB(x, y))

            // get the last bit as Char of the pixel blue color
            val blue = color.blue.toString(2).last()  // get blue color and apply LSB
            mInt.add(blue.digitToInt())
        }
    }

    return mInt.joinToString(separator = "")
}

fun encodeMessageToArray(message: String, password: String): String {
    // encode the text to ByteArray
    val byteArray: ByteArray = message.encodeToByteArray()

    // encode the password to ByteArray
    val byteArrayPassword: ByteArray = password.encodeToByteArray()

    val passwordSize = byteArrayPassword.size
    var iterator: Int

    // scramble the array with password
    for (index in byteArray.indices) {
        iterator = if (index < passwordSize) {
            index
        } else {
            (index - passwordSize) % passwordSize
        }
        byteArray[index] = byteArray[index] xor byteArrayPassword[iterator]
    }

    // add the termination bytes
    val newByteArray = byteArray + byteArrayOf(0x0, 0x0, 0x3)

    var aString = ""

    // convert the byte array to bits and everything as a string
    for (element in newByteArray) {
        aString += element.toString(2).padStart(8, '0')
    }
    return aString
}

fun decodeMessageFromArray(encodedMessage: String, password: String): String {
    // find the string terminator
    val subString = encodedMessage.substringBefore("000000000000000000000011")
    var plainText = ""

    // split the bytes into groups of 8 bit
    val chunkedString = subString.chunked(8)

    // encode the password to ByteArray
    val byteArrayPassword: ByteArray = password.encodeToByteArray()

    val passwordSize = byteArrayPassword.size
    var iterator: Int

    // decode each byte into char
    for (index in chunkedString.indices) {
        var aChar = chunkedString[index].toInt(2)

        // descramble the array with password
        iterator = if (index < passwordSize) {
            index
        } else {
            (index - passwordSize) % passwordSize
        }

        aChar = aChar xor byteArrayPassword[iterator].toInt()
        plainText += aChar.toChar()
    }

    return plainText
}

fun checkImageSize(myImage: BufferedImage, message: String): Boolean {
    // the message to encode must be: message length bytes times 8 bit plus 3 bytes
    val messageLength = message.length * 8 + 24
    val imageSize = myImage.width * myImage.width

    return imageSize >= messageLength
}

fun saveImage(myImage: BufferedImage, outputString: String): Boolean {
    // Create a file instance in order to write the image file
    val outputFileJpg = File(outputString)

    // Output the file
    // Create an image using the BufferedImage instance data
    return ImageIO.write(myImage, "png", outputFileJpg)
}