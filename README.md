# LmLinky

> An Android application for easily communicating with locally hosted AI models.

## Overview

LmLinky is an Android application that enables users to seamlessly interact with their locally hosted language models and AI services. It provides a user-friendly interface to send requests and receive responses from models running on your local machine or network.

## Features

- 🔗 **Local Model Integration** - Connect to locally hosted AI models
- 💬 **Easy Communication** - Simple interface for sending prompts and receiving responses
- 🚀 **Real-time Responses** - Fast communication with your local models
- 📱 **Android Native** - Optimized for Android devices
- 🎯 **User-Friendly UI** - Intuitive design for seamless interaction

## Prerequisites

Before you begin, ensure you have the following:

- **Android Studio** (latest version)
- **Android SDK** (API level 21 or higher)
- **Kotlin** (1.7.0 or higher)
- **JDK** (Java Development Kit 11 or higher)
- **A locally hosted AI model** (LLaMA, Mistral, GPT4All, or similar)

## Installation

### Clone the Repository

```bash
git clone https://github.com/Dewpg/LmLinky.git
cd LmLinky
```

### Build the Project

1. Open the project in Android Studio
2. Allow Gradle to sync and download dependencies
3. Build the project:
   ```bash
   ./gradlew build
   ```

### Run on Emulator or Device

```bash
./gradlew installDebug
```

Or use Android Studio's built-in run configuration.

## Usage

### Prerequisites for Local Model

1. **Set up a locally hosted model** using:
   - [Ollama](https://ollama.ai/) - Recommended for ease of use
   - [LM Studio](https://lmstudio.ai/)
   - [GPT4All](https://gpt4all.io/)
   - Any OpenAI-compatible API endpoint

2. **Configure the connection** in LmLinky:
   - Enter your local model's server address (e.g., `http://192.168.1.100:11434`)
   - Select or specify your model name
   - Save configuration

3. **Start chatting** with your model:
   - Type your prompt
   - Press send
   - Receive responses from your local model

### Example Configuration

```
Server: http://192.168.1.100:11434
Model: llama2
```

## Project Structure

```
LmLinky/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/
│   │   │   │   └── com/dewpg/lmlinky/
│   │   │   │       ├── MainActivity.kt
│   │   │   │       ├── viewmodel/
│   │   │   │       ├── ui/
│   │   │   │       └── api/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── build.gradle.kts
└── README.md
```

## Configuration

### Network Settings

- Configure your local model server address in the app settings
- Ensure your Android device is on the same network as your local model server
- For development, you can use `10.0.2.2` as localhost when using the Android emulator

### Model Configuration

- Specify the model name or endpoint
- Adjust timeout settings if needed
- Configure any authentication tokens if required

## API Communication

LmLinky communicates with your locally hosted model using standard HTTP requests (typically POST requests with JSON payloads).

Example endpoint:
```
POST http://[your-server]:11434/api/generate
Content-Type: application/json

{
  "model": "llama2",
  "prompt": "Your prompt here",
  "stream": false
}
```

## Dependencies

- **Kotlin Coroutines** - For asynchronous operations
- **Retrofit** - For HTTP networking
- **OkHttp** - HTTP client
- **Jetpack Compose** or **Material Design** - UI framework
- **LiveData/ViewModel** - Android Architecture Components

## Contributing

Contributions are welcome! Here's how you can help:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Troubleshooting

### Cannot connect to local model

- Verify the server address is correct
- Ensure your Android device is on the same network
- Check that your local model server is running
- Verify firewall settings allow connections

### No response from model

- Check the model name is correct
- Ensure the model is loaded on your local server
- Check network connectivity
- Review server logs for errors

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Author

**Dewpg**  
GitHub: [@Dewpg](https://github.com/Dewpg)

## Acknowledgments

- Special thanks to the open-source community for model hosting solutions
- Inspired by the need for easy local AI model interaction on Android

---

**Note:** This application requires a locally hosted AI model. Make sure your model server is running and accessible before using LmLinky.
